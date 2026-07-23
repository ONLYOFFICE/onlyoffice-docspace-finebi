package com.asc.fr.docspace.application.job;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileRetrievalService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * Reconciles the stored file links against both sides and drops the ones that are no longer valid:
 *
 * <ul>
 *   <li>the DocSpace file was deleted (a delete webhook we may have missed while down), or
 *   <li>the FineBI dataset was deleted (no event exists for that, so nothing else catches it).
 * </ul>
 *
 * <p>The probes are I/O bound and the shared worker pool is tiny, so a run does not walk the links
 * one blocking call at a time. It authenticates once, fires all DocSpace and FineBI probes
 * concurrently through the async HTTP client, and only blocks the (single, lock-holding) run thread
 * while it collects the answers — leaving the rest of the pool free. FineBI probes are
 * folder-scoped and shared across every dataset in the same pack, so records that live together
 * cost one call, not one per record. {@code maxPerRun} keeps each run small; the schedule runs more
 * often to compensate.
 */
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class SyncLinkReconciliationClusterJob implements ScheduledClusterJob {
  private static final int PAGE_SIZE = 100;

  private final SynchronizationLinkRegistry synchronizationLinkRegistry;
  private final SynchronizationSettings synchronizationSettings;
  private final DocSpaceFileRetrievalService docSpaceFiles;
  private final DocSpaceTenantService tenantService;
  private final FineDatasetService datasetService;
  private final FineSessionFactory sessionFactory;
  private final SyncLinkJobSchedule schedule;

  private List<FileSynchronizationRecord> collectTargets(long cutoff) {
    int budget = schedule.maxPerRun;

    String cursor = "";
    Set<String> seen = new HashSet<>();
    List<FileSynchronizationRecord> targets = new ArrayList<>();

    while (budget > 0) {
      int pageSize = Math.min(PAGE_SIZE, budget);
      List<FileSynchronizationRecord> page =
          synchronizationLinkRegistry.staleLinks(cutoff, cursor, pageSize);

      if (page.isEmpty()) break;

      int advanced = 0;
      for (FileSynchronizationRecord record : page) {
        cursor = record.getTableId();
        advanced++;

        if (!seen.add(cursor)) continue;
        if (record.getFileId().isEmpty()) continue; // No DocSpace file to probe

        targets.add(record);
        if (--budget <= 0) break;
      }

      if (page.size() < pageSize || advanced == 0) break;
    }

    return targets;
  }

  private Runnable keep(FileSynchronizationRecord record) {
    return () -> {
      try {
        synchronizationLinkRegistry.put(record);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  private Runnable remove(FileSynchronizationRecord record) {
    return () -> {
      try {
        synchronizationLinkRegistry.remove(record.getTableId());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  private CompletableFuture<Runnable> decide(
      FileSynchronizationRecord record,
      Map<String, CompletableFuture<Boolean>> fileProbes,
      Map<String, CompletableFuture<Set<String>>> folderProbes,
      FineSession session) {
    return fileProbes
        .get(record.getFileId())
        .thenCompose(
            docSpaceHasFile -> {
              if (!docSpaceHasFile) return CompletableFuture.completedFuture(remove(record));
              return folderProbes
                  .computeIfAbsent(
                      record.getFolderId(),
                      folderId -> datasetService.tableIdsInFolderAsync(folderId, session))
                  .thenApply(
                      tableIds ->
                          tableIds.contains(record.getTableId()) ? keep(record) : remove(record));
            });
  }

  private void reconcile(
      URL docSpaceUrl,
      DocSpaceAccountCredentials admin,
      FineSession session,
      List<FileSynchronizationRecord> targets) {
    Set<String> fileIds = new HashSet<>();

    for (FileSynchronizationRecord record : targets) fileIds.add(record.getFileId());

    Map<String, CompletableFuture<Boolean>> fileProbes;
    try {
      fileProbes = docSpaceFiles.fileExistenceProbes(docSpaceUrl, fileIds, admin);
    } catch (Exception ignored) {
      return; // could not authenticate, keep every link till next tick
    }

    Map<String, CompletableFuture<Set<String>>> folderProbes = new ConcurrentHashMap<>();
    List<CompletableFuture<Runnable>> decisions = new ArrayList<>(targets.size());

    for (FileSynchronizationRecord record : targets)
      decisions.add(decide(record, fileProbes, folderProbes, session));

    for (CompletableFuture<Runnable> decision : decisions) {
      try {
        decision.join().run();
      } catch (Exception ignore) {
        // No response, till next tick
      }
    }
  }

  @Override
  public String name() {
    return "link-cluster-reconciliation";
  }

  @Override
  public long initialDelayMillis() {
    return schedule.initialDelayMillis;
  }

  @Override
  public long periodMillis() {
    return schedule.periodMillis;
  }

  @Override
  public void run() {
    if (!tenantService.isConfigured()) return;

    String decisionBase = synchronizationSettings.loadDecisionBase();
    if (decisionBase.isEmpty()) return;

    long cutoff = System.currentTimeMillis() - schedule.staleAfterMillis;
    List<FileSynchronizationRecord> targets = collectTargets(cutoff);
    if (targets.isEmpty()) return;

    URL docSpaceUrl = new URL(tenantService.docSpaceUrl());
    DocSpaceAccountCredentials admin = tenantService.adminCredentials();
    FineSession session = sessionFactory.generateSession(decisionBase);

    reconcile(docSpaceUrl, admin, session, targets);
  }
}

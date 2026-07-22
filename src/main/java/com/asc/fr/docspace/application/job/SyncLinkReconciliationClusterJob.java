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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Reconciles the stored file links against both sides and drops the ones that are no longer valid:
 *
 * <ul>
 *   <li>the DocSpace file was deleted (a delete webhook we may have missed while down), or
 *   <li>the FineBI dataset was deleted (no event exists for that, so nothing else catches it).
 * </ul>
 */
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class SyncLinkReconciliationClusterJob implements ScheduledClusterJob {
  private static final int MAX_PER_RUN = 100;
  private static final int PAGE_SIZE = 100;

  private final SynchronizationLinkRegistry synchronizationLinkRegistry;
  private final SynchronizationSettings synchronizationSettings;
  private final DocSpaceFileRetrievalService docSpaceFiles;
  private final DocSpaceTenantService tenantService;
  private final FineDatasetService datasetService;
  private final FineSessionFactory sessionFactory;
  private final SyncLinkJobSchedule schedule;

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

    URL docSpaceUrl = new URL(tenantService.docSpaceUrl());
    DocSpaceAccountCredentials admin = tenantService.adminCredentials();
    FineSession session = sessionFactory.generateSession(decisionBase);

    long cutoff = System.currentTimeMillis() - schedule.staleAfterMillis;
    int budget = MAX_PER_RUN;
    String cursor = "";

    Set<String> seen = new HashSet<>();

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

        if (record.getFileId().isEmpty()) continue; // no DocSpace file to probe

        budget--;

        reconcile(docSpaceUrl, record, admin, session);

        if (budget <= 0) break;
      }

      if (page.size() < pageSize || advanced == 0) break;
    }
  }

  private void reconcile(
      URL docSpaceUrl,
      FileSynchronizationRecord record,
      DocSpaceAccountCredentials admin,
      FineSession session) {
    try {
      if (!docSpaceFiles.fileExists(docSpaceUrl, record.getFileId(), admin)) {
        synchronizationLinkRegistry.remove(record.getTableId()); // File doesn't exist in DocSpace
        return;
      }

      if (!datasetService.datasetExists(record.getTableId(), record.getFolderId(), session)) {
        synchronizationLinkRegistry.remove(
            record.getTableId()); // FineBI dataset is gone, remove it
        return;
      }

      synchronizationLinkRegistry.put(record); // Both links are alive, update timestamp
    } catch (Exception ignored) {
    }
  }
}

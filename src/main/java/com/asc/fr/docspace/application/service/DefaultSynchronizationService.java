package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.SynchronizationService;
import com.asc.fr.docspace.application.port.input.transfer.SynchronizationCommand;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileCommand;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.application.port.output.fr.transfer.*;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.Sheet;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.common.spreadsheet.Spreadsheet;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineDataset;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultSynchronizationService implements SynchronizationService {
  private static final long DOCSPACE_WRITE_DELAY_MS = 3_500;
  private final ConcurrentMap<String, TaskSchedulerService.Cancellable> pending =
      new ConcurrentHashMap<>();

  private final DocSpaceTenantService tenantService;
  private final DocSpaceSavedTenantService savedTenantService;
  private final DocSpaceFileDownloadService fileDownloadService;

  private final FineAttachmentService attachmentService;
  private final FineDatasetService datasetService;

  private final FineSessionFactory sessionFactory;
  private final TaskSchedulerService taskSchedulerService;
  private final SynchronizationEventPublisher eventPublisher;
  private final SynchronizationLinkRegistry synchronizationService;

  private static Set<String> trackedTableIds(List<FileSynchronizationRecord> entries) {
    Set<String> ids = new HashSet<>();
    for (FileSynchronizationRecord entry : entries)
      if (!entry.getTableId().isEmpty() && entry.getSheetId() > 0) ids.add(entry.getTableId());
    return ids;
  }

  private static Set<Integer> trackedSheetIds(List<FileSynchronizationRecord> entries) {
    Set<Integer> ids = new HashSet<>();
    for (FileSynchronizationRecord entry : entries)
      if (entry.getSheetId() > 0) ids.add(entry.getSheetId());
    return ids;
  }

  // EXPERIMENTAL: picks the folder most sibling datasets of this file already live in.
  private static String majoritySiblingFolderId(Map<String, FineDatasetLocation> locations) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (FineDatasetLocation location : locations.values())
      counts.merge(location.getFolderId(), 1, Integer::sum);

    String best = "";
    int bestCount = 0;
    for (Map.Entry<String, Integer> count : counts.entrySet())
      if (count.getValue() > bestCount) {
        best = count.getKey();
        bestCount = count.getValue();
      }

    return best;
  }

  // A file's links can belong to any tenant an admin has ever connected to (they're no longer
  // wiped on a tenant switch), so a resync must resolve each link's own tenant rather than
  // assuming whichever one is currently active — otherwise a file from tenant 2 could be
  // downloaded (and its dataset overwritten) using tenant 1's credentials.
  private Optional<DocSpaceTenantConfiguration> resolveTenant(String tenantUrl) {
    String currentUrl = tenantService.docSpaceUrl();

    if (tenantUrl.isEmpty() || tenantUrl.equals(currentUrl)) {
      if (currentUrl.isEmpty()) return Optional.empty();
      if (!tenantService.adminCredentials().isComplete()) return Optional.empty();
      return Optional.of(
          new DocSpaceTenantConfiguration(currentUrl, tenantService.adminCredentials()));
    }

    return savedTenantService.find(tenantUrl).filter(config -> config.getAdmin().isComplete());
  }

  // EXPERIMENTAL: sheets added to the workbook after import have no tracked link yet — auto-import
  // them here so they start syncing without a manual re-import via "Add Dataset".
  private void importNewSheets(
      List<FileSynchronizationRecord> entries,
      Spreadsheet workbook,
      Map<String, FineDatasetLocation> locations,
      String filename,
      byte[] content,
      FineSession session,
      String fileId,
      String tenantUrl) {
    Set<Integer> trackedSheetIds = trackedSheetIds(entries);
    List<Sheet> newSheets = new ArrayList<>();
    for (Sheet sheet : workbook.sheets())
      if (sheet.getSheetId() > 0 && !trackedSheetIds.contains(sheet.getSheetId()))
        newSheets.add(sheet);
    if (newSheets.isEmpty()) return;

    int budget = DocSpaceImporterService.MAX_SHEETS - entries.size();
    if (budget <= 0) return;

    if (newSheets.size() > budget) newSheets = newSheets.subList(0, budget);

    String folderId = majoritySiblingFolderId(locations);
    if (folderId.isEmpty()) return;

    try {
      String tableName = new DocSpaceSpreadsheet(filename).getTableName();
      FineAttachment attachment =
          attachmentService.uploadAttachment(
              FineUploadAttachmentCommand.builder().fileName(filename).content(content).build(),
              session);

      List<Integer> sheetIndices = new ArrayList<>(newSheets.size());
      for (Sheet sheet : newSheets) sheetIndices.add(workbook.indexOfSheetId(sheet.getSheetId()));

      List<FineDataset> created =
          datasetService.createDatasets(
              FineCreateDatasetCommand.builder()
                  .tableName(tableName)
                  .folderId(folderId)
                  .attachment(attachment)
                  .sheets(newSheets)
                  .sheetIndices(sheetIndices)
                  .build(),
              session);

      Map<Integer, String> hashBySheetId = new HashMap<>();
      for (Sheet sheet : newSheets)
        hashBySheetId.putIfAbsent(sheet.getSheetId(), sheet.getContentHash());

      for (FineDataset dataset : created)
        synchronizationService.put(
            new FileSynchronizationRecord(
                fileId,
                dataset.getTableId(),
                dataset.getSheetId(),
                hashBySheetId.getOrDefault(dataset.getSheetId(), ""),
                0L,
                tenantUrl));
    } catch (Exception ignored) {
      // Best-effort: a failed auto-import here is retried on the next webhook for this file.
    }
  }

  private void resyncForTenant(
      String decisionBase,
      String fileId,
      DocSpaceTenantConfiguration tenant,
      List<FileSynchronizationRecord> entries) {
    FineSession session = sessionFactory.generateSession(decisionBase);
    try {
      DocSpaceRawFile download =
          fileDownloadService.download(
              DocSpaceDownloadFileCommand.builder()
                  .docSpaceUrl(new URL(tenant.getUrl().getValue()))
                  .fileId(fileId)
                  .build(),
              tenant.getAdmin());

      byte[] content = download.getRawContent();
      if (content.length > DocSpaceImporterService.MAX_FILE_BYTES)
        throw new IOException("File exceeds the 50 MB import limit");

      String filename = new DocSpaceSpreadsheet(download.getFileName()).getFileName();
      Spreadsheet workbook = new Spreadsheet(filename, content);
      Map<String, FineDatasetLocation> locations =
          datasetService.locateDatasets(trackedTableIds(entries), session);

      List<FineReplaceDatasetCommand> commands = new ArrayList<>();
      Map<String, FileSynchronizationRecord> tableEntries = new HashMap<>();

      for (FileSynchronizationRecord entry : entries) {
        if (entry.getTableId().isEmpty()) continue;

        // Datasets are linked to their sheet only by the stable OOXML sheetId. A row without one
        // predates sheetId tracking (or came from a source no longer importable) and can only be
        // re-synced by re-importing — leave it untouched.
        if (entry.getSheetId() <= 0) continue;

        int sheetIndex = workbook.indexOfSheetId(entry.getSheetId());
        if (sheetIndex < 0) {
          // The sheet backing this dataset is gone from the workbook — drop the mapping and leave
          // the FineBI dataset orphaned.
          synchronizationService.remove(entry.getTableId());
          continue;
        }

        FineDatasetLocation location = locations.get(entry.getTableId());

        // Not found in any listable folder — the dataset was deleted or moved somewhere the packs
        // listing does not expose. Leave the mapping and retry on the next webhook rather than risk
        // dropping a dataset we merely could not see.
        if (location == null) continue;

        // Unchanged sheet data — no upload/preview/update.
        String currentHash = workbook.contentHash(entry.getSheetId());
        if (!entry.getContentHash().isEmpty() && entry.getContentHash().equals(currentHash))
          continue;

        tableEntries.put(entry.getTableId(), entry);
        commands.add(
            FineReplaceDatasetCommand.builder()
                .tableId(entry.getTableId())
                .folderId(location.getFolderId())
                .name(location.getName())
                .fileName(filename)
                .sheetIndex(sheetIndex)
                .build());
      }

      importNewSheets(
          entries,
          workbook,
          locations,
          filename,
          content,
          session,
          fileId,
          tenant.getUrl().getValue());

      List<FineReplaceOutcome> outcomes = new ArrayList<>(commands.size());
      for (FineReplaceDatasetCommand command : commands) {
        FineAttachment attachment =
            attachmentService.uploadAttachment(
                FineUploadAttachmentCommand.builder().fileName(filename).content(content).build(),
                session);
        outcomes.addAll(
            datasetService.replaceDatasets(
                Collections.singletonList(command), session, attachment));
      }

      for (FineReplaceOutcome outcome : outcomes) {
        String tableId = outcome.getTableId();
        FileSynchronizationRecord entry = tableEntries.get(tableId);
        FineDatasetLocation location = locations.get(tableId);
        String contentHash = entry == null ? "" : workbook.contentHash(entry.getSheetId());

        if (outcome.getStatus() == FineReplaceOutcome.Status.ABSENT) {
          synchronizationService.remove(tableId);
          continue;
        }

        if (outcome.getStatus() == FineReplaceOutcome.Status.FAILED) continue;

        datasetService.refreshDataset(
            FineRefreshDatasetCommand.builder()
                .folderId(location == null ? "" : location.getFolderId())
                .tableId(tableId)
                .build(),
            session);

        // Remember the sheet's content hash so the next webhook can skip it when data is unchanged.
        if (!contentHash.isEmpty() && !contentHash.equals(entry.getContentHash()))
          synchronizationService.put(entry.withContentHash(contentHash));

        // Notify with FineBI's own current name for the dataset (not our stored import name).
        eventPublisher.datasetUpdated(outcome.getName());
      }
    } catch (Exception ignored) {
      // TODO: Handle it somehow
    }
  }

  private void resync(String decisionBase, String fileId, String tenantHint) {
    List<FileSynchronizationRecord> entries = synchronizationService.findByFile(fileId);
    if (entries.isEmpty()) return;

    // A single fileId can carry links from more than one tenant (a numeric DocSpace file id isn't
    // globally unique across instances), so each tenant's entries are resynced separately, against
    // that tenant's own file and credentials.
    Map<String, List<FileSynchronizationRecord>> byTenant = new LinkedHashMap<>();
    for (FileSynchronizationRecord entry : entries)
      byTenant.computeIfAbsent(entry.getTenantUrl(), key -> new ArrayList<>()).add(entry);

    // The webhook's signature identified exactly which tenant sent this event (see
    // WebhookHttpHandler) — when that tenant has entries for this file, resync only those; a
    // same-fileId entry under a different tenant is an unrelated file on a different DocSpace
    // instance. Otherwise (tenant not identified, or it has no entries here) fall back to
    // resyncing every tenant that does.
    Map<String, List<FileSynchronizationRecord>> targets =
        !tenantHint.isEmpty() && byTenant.containsKey(tenantHint)
            ? Collections.singletonMap(tenantHint, byTenant.get(tenantHint))
            : byTenant;

    for (Map.Entry<String, List<FileSynchronizationRecord>> group : targets.entrySet()) {
      Optional<DocSpaceTenantConfiguration> tenant = resolveTenant(group.getKey());
      // No usable credentials for this tenant right now (e.g. its saved history was reset) —
      // best-effort, retried on the next webhook for this file.
      tenant.ifPresent(config -> resyncForTenant(decisionBase, fileId, config, group.getValue()));
    }
  }

  @Override
  public void schedule(SynchronizationCommand command) {
    String fileId = command.getFileId();
    String decisionBase = command.getDecisionBase();
    String tenantHint = command.getTenantUrl();
    if (synchronizationService.findByFile(fileId).isEmpty()) return;

    // Keyed by tenant too where known, so a debounced event for one tenant's file can't be
    // cancelled by an unrelated event for a different tenant's file that happens to share the
    // same numeric DocSpace id.
    String pendingKey = tenantHint.isEmpty() ? fileId : tenantHint + '|' + fileId;

    TaskSchedulerService.Cancellable replaced =
        pending.put(
            pendingKey,
            taskSchedulerService.schedule(
                DOCSPACE_WRITE_DELAY_MS,
                () -> {
                  pending.remove(pendingKey);
                  resync(decisionBase, fileId, tenantHint);
                }));

    if (replaced != null) replaced.cancel();
  }
}

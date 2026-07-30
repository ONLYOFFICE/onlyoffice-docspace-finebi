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
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.common.spreadsheet.Spreadsheet;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import com.asc.fr.docspace.domain.fr.FineAttachment;
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

  private void resync(String decisionBase, String fileId, DocSpaceAccountCredentials credentials) {
    List<FileSynchronizationRecord> entries = synchronizationService.findByFile(fileId);
    if (entries.isEmpty()) return;

    FineSession session = sessionFactory.generateSession(decisionBase);
    try {
      DocSpaceRawFile download =
          fileDownloadService.download(
              DocSpaceDownloadFileCommand.builder()
                  .docSpaceUrl(new URL(tenantService.docSpaceUrl()))
                  .fileId(fileId)
                  .build(),
              credentials);

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

  @Override
  public void schedule(SynchronizationCommand command) {
    String fileId = command.getFileId();
    String decisionBase = command.getDecisionBase();
    if (synchronizationService.findByFile(fileId).isEmpty()) return;

    if (!tenantService.isConfigured()) return;

    DocSpaceAccountCredentials credentials = tenantService.adminCredentials();
    if (!credentials.isComplete()) return;

    TaskSchedulerService.Cancellable replaced =
        pending.put(
            fileId,
            taskSchedulerService.schedule(
                DOCSPACE_WRITE_DELAY_MS,
                () -> {
                  pending.remove(fileId);
                  resync(decisionBase, fileId, credentials);
                }));

    if (replaced != null) replaced.cancel();
  }
}

package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.exception.DatasetAbsentException;
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
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineUploadAttachmentCommand;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.inject.Inject;
import java.io.IOException;
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
  private final com.asc.fr.docspace.domain.SynchronizationService synchronizationService;

  /**
   * Replaces the tracked dataset's source in-place, keeping its UUID (and all dashboards
   * referencing it) intact.
   *
   * <p><b>CLEANUP SEMANTICS (FineBI side)</b> — the DocSpace side lives in WebhookHttpHandler. A
   * re-sync only ever updates the dataset by its stored UUID; there is deliberately <b>no
   * create-dataset fallback</b> anymore. The old fallback resurrected datasets a user had deleted
   * in FineBI and produced duplicates when only the UUID was stale. Instead:
   *
   * <ul>
   *   <li>Replace succeeded → normal sync.
   *   <li>Replace threw {@link DatasetAbsentException} — FineBI's own "FineTableAbsentException"
   *       answer, the authoritative signal that the user deleted the dataset in FineBI → we honor
   *       that by removing the registry entry. Re-importing the file is the explicit way to resume
   *       syncing.
   *   <li>Replace failed with any other error (transient FineBI or network problem) → keep the
   *       entry and rethrow; the next webhook event retries.
   * </ul>
   *
   * @return true when the dataset was actually updated; false when the entry was unregistered
   *     instead.
   */
  private boolean reimport(
      FineSession session,
      FileSynchronizationRecord entry,
      String fileId,
      String fileName,
      byte[] content)
      throws IOException {
    String filename = new DocSpaceSpreadsheet(fileName).getFileName();
    if (content.length > DocSpaceImporterService.MAX_FILE_BYTES)
      throw new IOException("File exceeds the 50 MB import limit");

    String uuid = entry.getTableId();
    if (uuid.isEmpty()) {
      // No UUID means the original import response carried none — we
      // cannot replace in-place, and creating by name would duplicate.
      // Unregister; the user re-imports to get a properly tracked entry.
      synchronizationService.remove(fileId);
      return false;
    }

    FineAttachment attachment =
        attachmentService.uploadAttachment(
            FineUploadAttachmentCommand.builder().fileName(filename).content(content).build(),
            session);

    try {
      datasetService.replaceDataset(
          FineReplaceDatasetCommand.builder()
              .tableId(uuid)
              .tableName(entry.getTableName())
              .folderId(entry.getFolderId())
              .fileName(filename)
              .build(),
          session,
          attachment);
    } catch (DatasetAbsentException e) {
      synchronizationService.remove(fileId);
      return false;
    }

    // Any other IOException propagates: transient failure — entry kept,
    // the next webhook event retries.
    // Tell Spider to re-extract data from the new source immediately so
    // charts reflect the change on next open rather than on next lazy load.
    datasetService.refreshDataset(
        FineRefreshDatasetCommand.builder().folderId(entry.getFolderId()).tableId(uuid).build(),
        session);

    return true;
  }

  private void resync(
      String decisionBase,
      String fileId,
      FileSynchronizationRecord entry,
      DocSpaceAccountCredentials credentials) {
    FineSession session = sessionFactory.generateSession(decisionBase);
    try {
      DocSpaceRawFile download =
          fileDownloadService.download(
              DocSpaceDownloadFileCommand.builder()
                  .docSpaceUrl(new URL(tenantService.docSpaceUrl()))
                  .fileId(fileId)
                  .build(),
              credentials);
      boolean synced =
          reimport(session, entry, fileId, download.getFileName(), download.getRawContent());
      if (synced) eventPublisher.datasetUpdated(entry.getTableName());
    } catch (Exception ignored) {
      // TODO: Handle it somehow
    }
  }

  @Override
  public void schedule(SynchronizationCommand command) {
    String fileId = command.getFileId();
    String decisionBase = command.getDecisionBase();
    FileSynchronizationRecord entry = synchronizationService.find(fileId);
    if (entry == null) return;

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
                  resync(decisionBase, fileId, entry, credentials);
                }));

    if (replaced != null) replaced.cancel();
  }
}

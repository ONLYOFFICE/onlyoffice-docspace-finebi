package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.ImportFileCommand;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileCommand;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileFromUrlCommand;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineUploadAttachmentCommand;
import com.asc.fr.docspace.domain.SynchronizationService;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

// TODO: The service seems brittle. Make it more reliable
// TODO: Make sure that there is caching for multiple sequential calls (caching must work well in a
// cluster of FineBI). Must be short-lived
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceImporterService implements DocSpaceImporterService {
  private static final String DOCSPACE_PACK = "DocSpace";

  private final DocSpaceTenantService tenantService;
  private final DocSpaceUserAccountService userAccountService;
  private final DocSpaceFileDownloadService fileDownloadService;

  private final FineFolderService folderService;
  private final FineDatasetService datasetService;
  private final FineAttachmentService attachmentService;

  private final WebhookRegistrar webhookRegistrar;
  private final TaskSchedulerService taskSchedulerService;
  private final SynchronizationService synchronizationService;

  // TODO: Get content-length before the real download
  private DocSpaceRawFile downloadSource(
      ImportFileCommand command, DocSpaceAccountCredentials credentials) throws IOException {
    String viewUrl = command.getViewUrl() == null ? "" : command.getViewUrl();
    String fileName = command.getFileName() == null ? "" : command.getFileName();
    URL docSpaceUrl = new URL(tenantService.docSpaceUrl());
    if (!viewUrl.isEmpty()) {
      String hint = fileName.isEmpty() ? command.getFileId() + ".xlsx" : fileName;
      return fileDownloadService.downloadFromUrl(
          DocSpaceDownloadFileFromUrlCommand.builder()
              .docSpaceUrl(docSpaceUrl)
              .viewUrl(viewUrl)
              .fileNameHint(hint)
              .build(),
          credentials);
    }

    DocSpaceRawFile download =
        fileDownloadService.download(
            DocSpaceDownloadFileCommand.builder()
                .docSpaceUrl(docSpaceUrl)
                .fileId(command.getFileId())
                .build(),
            credentials);

    return fileName.isEmpty() ? download : new DocSpaceRawFile(fileName, download.getRawContent());
  }

  private void ensureWebhookRegistered(String callbackUrl) throws IOException {
    synchronizationService.storeCallbackUrl(callbackUrl);
    String secret = synchronizationService.ensureSecret();
    taskSchedulerService.run(
        () ->
            webhookRegistrar.ensureRegistered(
                new URL(tenantService.docSpaceUrl()),
                new URL(callbackUrl),
                secret,
                tenantService.adminCredentials()));
  }

  @Override
  public String importFile(ImportFileCommand command, FineSession session) throws IOException {
    DocSpaceAccountCredentials credentials = userAccountService.credentials(command.getUserName());
    DocSpaceSpreadsheet spreadsheet;
    byte[] rawFile;

    try {
      DocSpaceRawFile download = downloadSource(command, credentials);
      rawFile = download.getRawContent();
      spreadsheet = new DocSpaceSpreadsheet(download.getFileName());
    } catch (IOException e) {
      throw new IOException("DocSpace download failed: " + e.getMessage());
    }

    if (rawFile.length > MAX_FILE_BYTES)
      throw new IOException("File exceeds the 50 MB import limit");

    String tableName = spreadsheet.getTableName();
    String filename = spreadsheet.getFileName();

    try {
      FineAttachment attachment =
          attachmentService.uploadAttachment(
              FineUploadAttachmentCommand.builder().fileName(filename).content(rawFile).build(),
              session);

      String folderId = command.getFolderId() == null ? "" : command.getFolderId();
      if (folderId.isEmpty()) folderId = folderService.ensureFolder(DOCSPACE_PACK, session);

      String tableId =
          datasetService.createDataset(
              FineCreateDatasetCommand.builder()
                  .tableName(tableName)
                  .folderId(folderId)
                  .attachment(attachment)
                  .build(),
              session);

      synchronizationService.put(
          command.getFileId(), new FileSynchronizationRecord(tableName, folderId, tableId));

      ensureWebhookRegistered(command.getCallbackUrl());
      return tableName;
    } catch (IOException e) {
      throw new IOException("FineBI dataset creation failed: " + e.getMessage());
    }
  }
}

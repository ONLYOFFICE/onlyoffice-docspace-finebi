package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.exception.ImportRejectedException;
import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.ImportFileCommand;
import com.asc.fr.docspace.application.port.output.CachingService;
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
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.Sheet;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.common.spreadsheet.Spreadsheet;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineDataset;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultDocSpaceImporterService implements DocSpaceImporterService {
  private static final String DOCSPACE_PACK = "DocSpace";
  private static final long WEBHOOK_ENSURE_TTL_SECONDS = 30;
  private static final long WEBHOOK_ENSURE_MAX_ENTRIES = 100;

  private final DocSpaceTenantService tenantService;
  private final DocSpaceUserAccountService userAccountService;
  private final DocSpaceFileDownloadService fileDownloadService;

  private final FineFolderService folderService;
  private final FineDatasetService datasetService;
  private final FineAttachmentService attachmentService;

  private final WebhookRegistrar webhookRegistrar;
  private final TaskSchedulerService taskSchedulerService;
  private final SynchronizationSettings synchronizationSettings;
  private final SynchronizationLinkRegistry synchronizationLinkRegistry;

  private final CachingService.Cache<String, Boolean> cache;

  @Inject
  public DefaultDocSpaceImporterService(
      DocSpaceTenantService tenantService,
      DocSpaceUserAccountService userAccountService,
      DocSpaceFileDownloadService fileDownloadService,
      FineFolderService folderService,
      FineDatasetService datasetService,
      FineAttachmentService attachmentService,
      WebhookRegistrar webhookRegistrar,
      TaskSchedulerService taskSchedulerService,
      SynchronizationSettings synchronizationSettings,
      SynchronizationLinkRegistry synchronizationLinkRegistry,
      CachingService cache) {
    this.tenantService = tenantService;
    this.userAccountService = userAccountService;
    this.fileDownloadService = fileDownloadService;
    this.folderService = folderService;
    this.datasetService = datasetService;
    this.attachmentService = attachmentService;
    this.webhookRegistrar = webhookRegistrar;
    this.taskSchedulerService = taskSchedulerService;
    this.synchronizationSettings = synchronizationSettings;
    this.synchronizationLinkRegistry = synchronizationLinkRegistry;
    this.cache =
        cache.create("webhook-ensure", WEBHOOK_ENSURE_TTL_SECONDS, WEBHOOK_ENSURE_MAX_ENTRIES);
  }

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
    if (callbackUrl == null || callbackUrl.isEmpty()) return;
    if (cache.get(callbackUrl) != null) return;

    URL docSpaceUrl = new URL(tenantService.docSpaceUrl());
    URL callback = new URL(callbackUrl);

    synchronizationSettings.storeCallbackUrl(callbackUrl);
    String secret = synchronizationSettings.ensureSecret();
    taskSchedulerService.run(
        () ->
            webhookRegistrar.ensureRegistered(
                docSpaceUrl, callback, secret, tenantService.adminCredentials()));

    cache.put(callbackUrl, Boolean.TRUE);
  }

  @Override
  public int importFile(ImportFileCommand command, FineSession session) throws IOException {
    DocSpaceAccountCredentials credentials = userAccountService.credentials(command.getUserName());
    if (!credentials.isComplete())
      throw new IOException("No DocSpace login is stored for user " + command.getUserName());

    DocSpaceRawFile download;
    try {
      download = downloadSource(command, credentials);
    } catch (IOException e) {
      throw new IOException("DocSpace download failed: " + e.getMessage(), e);
    }

    byte[] rawFile = download.getRawContent();
    if (rawFile.length > MAX_FILE_BYTES)
      throw new ImportRejectedException(
          "import.error.tooLarge",
          "File exceeds the 50 MB import limit",
          Collections.singletonMap("maxMb", MAX_FILE_BYTES / (1024 * 1024)));

    DocSpaceSpreadsheet spreadsheet = new DocSpaceSpreadsheet(download.getFileName());
    String tableName = spreadsheet.getTableName();
    String filename = spreadsheet.getFileName();
    if (!filename.toLowerCase().endsWith(".xlsx"))
      throw new ImportRejectedException(
          "import.error.notXlsx", "Only .xlsx workbooks can be imported");

    List<Sheet> sheets = new Spreadsheet(filename, rawFile).sheets();
    if (sheets.size() > MAX_SHEETS) {
      Map<String, Object> params = new HashMap<>();
      params.put("count", sheets.size());
      params.put("max", MAX_SHEETS);
      throw new ImportRejectedException(
          "import.error.tooManySheets",
          "The workbook has " + sheets.size() + " sheets; the import limit is " + MAX_SHEETS,
          params);
    }

    String folderId;
    List<FineDataset> datasets;
    try {
      FineAttachment attachment =
          attachmentService.uploadAttachment(
              FineUploadAttachmentCommand.builder().fileName(filename).content(rawFile).build(),
              session);

      folderId = command.getFolderId() == null ? "" : command.getFolderId();
      if (folderId.isEmpty()) folderId = folderService.ensureFolder(DOCSPACE_PACK, session);

      datasets =
          datasetService.createDatasets(
              FineCreateDatasetCommand.builder()
                  .tableName(tableName)
                  .folderId(folderId)
                  .attachment(attachment)
                  .sheets(sheets)
                  .build(),
              session);
    } catch (ImportRejectedException e) {
      throw e;
    } catch (IOException e) {
      throw new IOException("FineBI dataset creation failed: " + e.getMessage(), e);
    }

    try {
      synchronizationLinkRegistry.removeByFile(command.getFileId());
      Map<Integer, String> sheetHash = new HashMap<>();
      for (Sheet sheet : sheets)
        if (sheet.getSheetId() > 0)
          sheetHash.putIfAbsent(sheet.getSheetId(), sheet.getContentHash());
      for (FineDataset dataset : datasets) {
        String hash = sheetHash.getOrDefault(dataset.getSheetId(), "");
        synchronizationLinkRegistry.put(
            new FileSynchronizationRecord(
                command.getFileId(), dataset.getTableId(), dataset.getSheetId(), hash, 0L));
      }

      synchronizationSettings.storeDecisionBase(session.getBaseUrl().getValue());
      ensureWebhookRegistered(command.getCallbackUrl());
    } catch (Exception bookkeeping) {
      // The dataset already exists; failing the import now would push the user to
      // retry and duplicate it. A lost record or registration only pauses webhook
      // syncs until the next import of this file redoes the bookkeeping.
    }

    return datasets.size();
  }
}

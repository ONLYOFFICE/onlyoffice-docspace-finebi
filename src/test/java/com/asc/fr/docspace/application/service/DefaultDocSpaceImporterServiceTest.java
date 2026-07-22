package com.asc.fr.docspace.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.transfer.ImportFileCommand;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultDocSpaceImporterServiceTest {
  private static final DocSpaceAccountCredentials USER_CREDS =
      new DocSpaceAccountCredentials("user@example.com", "1", "hash");
  private static final FineSession SESSION =
      new FineSession("http://localhost:37799/webroot/decision", "fine_auth_token=token");

  private TestPorts.InMemoryDocSpaceUserAccountService userAccountService;
  private TestPorts.InMemorySynchronizationService synchronizationService;
  private TestPorts.InMemoryDocSpaceTenantService tenantService;
  private TestPorts.RecordingWebhookRegistrar webhookRegistrar;
  private TestPorts.RecordingFineService recordingFineService;
  private DocSpaceImporterService importerService;
  private TestPorts.StubFileStorage fileStorage;

  @BeforeEach
  void setUp() {
    tenantService = new TestPorts.InMemoryDocSpaceTenantService();
    tenantService.config =
        new DocSpaceTenantConfiguration("https://docspace.example.com", USER_CREDS);
    userAccountService = new TestPorts.InMemoryDocSpaceUserAccountService();
    userAccountService.credentials.put("alice", USER_CREDS);
    fileStorage = new TestPorts.StubFileStorage();
    recordingFineService = new TestPorts.RecordingFineService();
    synchronizationService = new TestPorts.InMemorySynchronizationService();
    webhookRegistrar = new TestPorts.RecordingWebhookRegistrar();

    importerService =
        new DefaultDocSpaceImporterService(
            new DefaultDocSpaceTenantService(tenantService),
            new DefaultDocSpaceUserAccountService(userAccountService),
            fileStorage,
            recordingFineService,
            recordingFineService,
            recordingFineService,
            webhookRegistrar,
            new TestPorts.ImmediateTaskSchedulerService(),
            synchronizationService,
            synchronizationService,
            new TestPorts.InMemoryCachingService());
  }

  private static ImportFileCommand.ImportFileCommandBuilder command() {
    return ImportFileCommand.builder()
        .userName("alice")
        .fileId("22")
        .fileName("Report.xlsx")
        .viewUrl("")
        .folderId("folder-1")
        .callbackUrl("https://fr.example.com/decision/url/webook");
  }

  @Nested
  class WhenImportSucceeds {
    @Test
    void givenValidCommand_whenImportingFile_thenRecordsSyncEntryAndRegistersWebhook()
        throws IOException {
      String dataset = importerService.importFile(command().build(), SESSION);

      assertEquals("Report", dataset);
      assertEquals(1, recordingFineService.createDatasetCalls.size());
      assertEquals("Report", recordingFineService.createDatasetCalls.get(0).getTableName());
      assertEquals("folder-1", recordingFineService.createDatasetCalls.get(0).getFolderId());

      FileSynchronizationRecord entry = synchronizationService.find("22");
      assertNotNull(entry);
      assertEquals("uuid-created", entry.getTableId());
      assertEquals("folder-1", entry.getFolderId());

      String callbackUrl = synchronizationService.loadCallbackUrl();
      assertEquals("https://fr.example.com/decision/url/webook", callbackUrl);
      assertTrue(webhookRegistrar.registered.contains(callbackUrl));
    }

    @Test
    void givenEmptyFolderIdInCommand_whenImportingFile_thenFallsBackToDocSpacePackage()
        throws IOException {
      importerService.importFile(command().folderId("").build(), SESSION);

      assertEquals(1, recordingFineService.ensureFolderCalls.size());
      assertEquals("DocSpace", recordingFineService.ensureFolderCalls.get(0));
      assertEquals("folder-1", recordingFineService.createDatasetCalls.get(0).getFolderId());
    }

    @Test
    void givenSequentialImports_whenImportingFiles_thenWebhookIsEnsuredOnlyOnce()
        throws IOException {
      importerService.importFile(command().build(), SESSION);
      importerService.importFile(command().fileId("23").build(), SESSION);

      assertEquals(2, recordingFineService.createDatasetCalls.size());
      assertEquals(1, webhookRegistrar.registered.size());
    }

    @Test
    void givenSyncBookkeepingFails_whenImportingFile_thenDatasetIsStillReturned()
        throws IOException {
      synchronizationService.failPutWith = new IOException("registry down");

      String dataset = importerService.importFile(command().build(), SESSION);

      assertEquals("Report", dataset);
      assertEquals(1, recordingFineService.createDatasetCalls.size());
    }

    @Test
    void givenNoCallbackUrl_whenImportingFile_thenImportSucceedsWithoutWebhook()
        throws IOException {
      String dataset = importerService.importFile(command().callbackUrl(null).build(), SESSION);

      assertEquals("Report", dataset);
      assertTrue(webhookRegistrar.registered.isEmpty());
    }
  }

  @Nested
  class WhenImportFails {
    @Test
    void givenNoStoredLogin_whenImportingFile_thenThrowsBeforeDownloading() {
      IOException e =
          assertThrows(
              IOException.class,
              () -> importerService.importFile(command().userName("bob").build(), SESSION));

      assertEquals("No DocSpace login is stored for user bob", e.getMessage());
      assertNull(fileStorage.lastDownloadedFileId);
      assertTrue(recordingFineService.noCalls());
    }

    @Test
    void givenStorageDownloadFails_whenImportingFile_thenThrowsWithContext() {
      fileStorage.failWith = new IOException("example");

      IOException e =
          assertThrows(
              IOException.class, () -> importerService.importFile(command().build(), SESSION));

      assertEquals("DocSpace download failed: example", e.getMessage());
      assertTrue(recordingFineService.noCalls());
    }

    @Test
    void givenFileExceedsSizeLimit_whenImportingFile_thenThrowsWithSizeLimitMessage() {
      fileStorage.next =
          new DocSpaceRawFile("big.xlsx", new byte[DocSpaceImporterService.MAX_FILE_BYTES + 1]);

      IOException e =
          assertThrows(
              IOException.class, () -> importerService.importFile(command().build(), SESSION));

      assertEquals("File exceeds the 50 MB import limit", e.getMessage());
    }
  }
}

package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.ImportFileCommand;
import com.asc.fr.docspace.application.port.output.CachingService;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultDocSpaceImporterServiceTest {
  private static final String CALLBACK = "https://fr.example.com/decision/url/webook";
  private static final DocSpaceAccountCredentials USER_CREDENTIALS =
      new DocSpaceAccountCredentials("user@example.com", "1", "hash");
  private static final FineSession SESSION =
      new FineSession("http://localhost:37799/webroot/decision", "fine_auth_token=token");

  @Mock private DocSpaceTenantService tenantService;
  @Mock private DocSpaceUserAccountService userAccountService;
  @Mock private DocSpaceFileDownloadService fileDownloadService;
  @Mock private FineFolderService folderService;
  @Mock private FineDatasetService datasetService;
  @Mock private FineAttachmentService attachmentService;
  @Mock private WebhookRegistrar webhookRegistrar;
  @Mock private TaskSchedulerService taskScheduler;
  @Mock private SynchronizationSettings synchronizationSettings;
  @Mock private SynchronizationLinkRegistry synchronizationLinkRegistry;
  @Mock private CachingService cachingService;
  @Mock private CachingService.Cache<String, Boolean> cache;

  private DocSpaceImporterService importerService;

  @BeforeEach
  void setUp() throws IOException {
    Map<String, Boolean> cacheStore = new HashMap<>();
    lenient().when(cache.get(any())).thenAnswer(i -> cacheStore.get(i.<String>getArgument(0)));
    lenient()
        .doAnswer(i -> cacheStore.put(i.getArgument(0), i.getArgument(1)))
        .when(cache)
        .put(any(), any());
    when(cachingService.<String, Boolean>create(any(), anyLong(), anyLong())).thenReturn(cache);

    lenient().when(userAccountService.credentials("alice")).thenReturn(USER_CREDENTIALS);
    lenient().when(tenantService.docSpaceUrl()).thenReturn("https://docspace.example.com");
    lenient()
        .when(fileDownloadService.download(any(), any()))
        .thenReturn(new DocSpaceRawFile("data.xlsx", new byte[] {1, 2, 3}));
    lenient()
        .when(attachmentService.uploadAttachment(any(), any()))
        .thenReturn(new FineAttachment("attach-1"));
    lenient().when(folderService.ensureFolder(any(), any())).thenReturn("folder-1");
    lenient().when(datasetService.createDataset(any(), any())).thenReturn("uuid-created");
    lenient().when(synchronizationSettings.ensureSecret()).thenReturn("TestSecret123");
    lenient().doAnswer(i -> runTask(i.getArgument(0))).when(taskScheduler).run(any());

    importerService =
        new DefaultDocSpaceImporterService(
            tenantService,
            userAccountService,
            fileDownloadService,
            folderService,
            datasetService,
            attachmentService,
            webhookRegistrar,
            taskScheduler,
            synchronizationSettings,
            synchronizationLinkRegistry,
            cachingService);
  }

  private static ImportFileCommand.ImportFileCommandBuilder command() {
    return ImportFileCommand.builder()
        .userName("alice")
        .fileId("1")
        .fileName("Report.xlsx")
        .viewUrl("")
        .folderId("folder-1")
        .callbackUrl(CALLBACK);
  }

  private static Object runTask(Runnable task) {
    task.run();
    return null;
  }

  @Nested
  class WhenImportSucceeds {
    @Test
    void givenValidCommand_whenImportingFile_thenRecordsSyncEntryAndRegistersWebhook()
        throws IOException {
      String dataset = importerService.importFile(command().build(), SESSION);

      assertThat(dataset).isEqualTo("Report");

      ArgumentCaptor<FineCreateDatasetCommand> created =
          ArgumentCaptor.forClass(FineCreateDatasetCommand.class);
      verify(datasetService).createDataset(created.capture(), any());
      assertThat(created.getValue().getTableName()).isEqualTo("Report");
      assertThat(created.getValue().getFolderId()).isEqualTo("folder-1");

      ArgumentCaptor<FileSynchronizationRecord> stored =
          ArgumentCaptor.forClass(FileSynchronizationRecord.class);
      verify(synchronizationLinkRegistry).put(stored.capture());
      assertThat(stored.getValue().getFileId()).isEqualTo("1");
      assertThat(stored.getValue().getTableId()).isEqualTo("uuid-created");
      assertThat(stored.getValue().getFolderId()).isEqualTo("folder-1");

      verify(synchronizationSettings).storeCallbackUrl(CALLBACK);
      ArgumentCaptor<URL> callback = ArgumentCaptor.forClass(URL.class);
      verify(webhookRegistrar).ensureRegistered(any(), callback.capture(), any(), any());
      assertThat(callback.getValue().getValue()).isEqualTo(CALLBACK);
    }

    @Test
    void givenEmptyFolderIdInCommand_whenImportingFile_thenFallsBackToDocSpacePackage()
        throws IOException {
      importerService.importFile(command().folderId("").build(), SESSION);

      verify(folderService).ensureFolder("DocSpace", SESSION);
      ArgumentCaptor<FineCreateDatasetCommand> created =
          ArgumentCaptor.forClass(FineCreateDatasetCommand.class);
      verify(datasetService).createDataset(created.capture(), any());
      assertThat(created.getValue().getFolderId()).isEqualTo("folder-1");
    }

    @Test
    void givenSequentialImports_whenImportingFiles_thenWebhookIsEnsuredOnlyOnce()
        throws IOException {
      importerService.importFile(command().build(), SESSION);
      importerService.importFile(command().fileId("2").build(), SESSION);

      verify(datasetService, times(2)).createDataset(any(), any());
      verify(webhookRegistrar, times(1)).ensureRegistered(any(), any(), any(), any());
    }

    @Test
    void givenSyncBookkeepingFails_whenImportingFile_thenDatasetIsStillReturned()
        throws IOException {
      doThrow(new IOException("registry down")).when(synchronizationLinkRegistry).put(any());

      String dataset = importerService.importFile(command().build(), SESSION);

      assertThat(dataset).isEqualTo("Report");
      verify(datasetService).createDataset(any(), any());
    }

    @Test
    void givenNoCallbackUrl_whenImportingFile_thenImportSucceedsWithoutWebhook()
        throws IOException {
      String dataset = importerService.importFile(command().callbackUrl(null).build(), SESSION);

      assertThat(dataset).isEqualTo("Report");
      verifyNoInteractions(webhookRegistrar);
    }
  }

  @Nested
  class WhenImportFails {
    @Test
    void givenNoStoredLogin_whenImportingFile_thenThrowsBeforeDownloading() {
      when(userAccountService.credentials("bob")).thenReturn(DocSpaceAccountCredentials.empty());

      assertThatThrownBy(
              () -> importerService.importFile(command().userName("bob").build(), SESSION))
          .isInstanceOf(IOException.class)
          .hasMessage("No DocSpace login is stored for user bob");

      verifyNoInteractions(fileDownloadService, folderService, datasetService, attachmentService);
    }

    @Test
    void givenStorageDownloadFails_whenImportingFile_thenThrowsWithContext() throws IOException {
      when(fileDownloadService.download(any(), any())).thenThrow(new IOException("example"));

      assertThatThrownBy(() -> importerService.importFile(command().build(), SESSION))
          .isInstanceOf(IOException.class)
          .hasMessage("DocSpace download failed: example");

      verifyNoInteractions(folderService, datasetService, attachmentService);
    }

    @Test
    void givenFileExceedsSizeLimit_whenImportingFile_thenThrowsWithSizeLimitMessage()
        throws IOException {
      when(fileDownloadService.download(any(), any()))
          .thenReturn(
              new DocSpaceRawFile(
                  "big.xlsx", new byte[DocSpaceImporterService.MAX_FILE_BYTES + 1]));

      assertThatThrownBy(() -> importerService.importFile(command().build(), SESSION))
          .isInstanceOf(IOException.class)
          .hasMessage("File exceeds the 50 MB import limit");
    }
  }
}

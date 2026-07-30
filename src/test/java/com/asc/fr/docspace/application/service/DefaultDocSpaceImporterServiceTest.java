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

import com.asc.fr.docspace.application.exception.ImportRejectedException;
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
import com.asc.fr.docspace.domain.fr.FineDataset;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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

  private static byte[] workbookWithSheets(int count) {
    StringBuilder sheetsXml = new StringBuilder();
    StringBuilder relsXml = new StringBuilder();
    Map<String, String> entries = new LinkedHashMap<>();
    for (int i = 1; i <= count; i++) {
      sheetsXml
          .append("<sheet name=\"S")
          .append(i)
          .append("\" sheetId=\"")
          .append(i)
          .append("\" r:id=\"rId")
          .append(i)
          .append("\"/>");
      relsXml.append(
          "<Relationship Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet"
              + i
              + ".xml\" Id=\"rId"
              + i
              + "\"/>");
      entries.put(
          "xl/worksheets/sheet" + i + ".xml",
          "<?xml version=\"1.0\"?><worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>H</t></is></c></row></sheetData></worksheet>");
    }
    entries.put(
        "xl/workbook.xml",
        "<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
            + sheetsXml
            + "</sheets></workbook>");
    entries.put("xl/_rels/workbook.xml.rels", "<Relationships>" + relsXml + "</Relationships>");

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return bytes.toByteArray();
  }

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
    lenient()
        .when(datasetService.createDatasets(any(), any()))
        .thenReturn(Collections.singletonList(new FineDataset("", 0, "Report", "uuid-created")));
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
      int count = importerService.importFile(command().build(), SESSION);

      assertThat(count).isEqualTo(1);

      ArgumentCaptor<FineCreateDatasetCommand> created =
          ArgumentCaptor.forClass(FineCreateDatasetCommand.class);
      verify(datasetService).createDatasets(created.capture(), any());
      assertThat(created.getValue().getTableName()).isEqualTo("Report");
      assertThat(created.getValue().getFolderId()).isEqualTo("folder-1");

      verify(synchronizationLinkRegistry).removeByFile("1");
      ArgumentCaptor<FileSynchronizationRecord> stored =
          ArgumentCaptor.forClass(FileSynchronizationRecord.class);
      verify(synchronizationLinkRegistry).put(stored.capture());
      assertThat(stored.getValue().getFileId()).isEqualTo("1");
      assertThat(stored.getValue().getTableId()).isEqualTo("uuid-created");

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
      verify(datasetService).createDatasets(created.capture(), any());
      assertThat(created.getValue().getFolderId()).isEqualTo("folder-1");
    }

    @Test
    void givenMultiSheetWorkbook_whenImportingFile_thenStoresOneRecordPerSheetAndSummarizes()
        throws IOException {
      when(datasetService.createDatasets(any(), any()))
          .thenReturn(
              Arrays.asList(
                  new FineDataset("Sales", 2, "Report_Sales", "uuid-sales"),
                  new FineDataset("Inventory", 3, "Report_Inventory", "uuid-inventory")));

      int count = importerService.importFile(command().build(), SESSION);

      assertThat(count).isEqualTo(2);

      ArgumentCaptor<FileSynchronizationRecord> stored =
          ArgumentCaptor.forClass(FileSynchronizationRecord.class);
      verify(synchronizationLinkRegistry, times(2)).put(stored.capture());
      List<FileSynchronizationRecord> records = stored.getAllValues();
      assertThat(records)
          .extracting(FileSynchronizationRecord::getTableId)
          .containsExactly("uuid-sales", "uuid-inventory");
      assertThat(records).extracting(FileSynchronizationRecord::getSheetId).containsExactly(2, 3);
      assertThat(records).allSatisfy(record -> assertThat(record.getFileId()).isEqualTo("1"));
    }

    @Test
    void givenSequentialImports_whenImportingFiles_thenWebhookIsEnsuredOnlyOnce()
        throws IOException {
      importerService.importFile(command().build(), SESSION);
      importerService.importFile(command().fileId("2").build(), SESSION);

      verify(datasetService, times(2)).createDatasets(any(), any());
      verify(webhookRegistrar, times(1)).ensureRegistered(any(), any(), any(), any());
    }

    @Test
    void givenSyncBookkeepingFails_whenImportingFile_thenDatasetIsStillReturned()
        throws IOException {
      doThrow(new IOException("registry down")).when(synchronizationLinkRegistry).put(any());

      int count = importerService.importFile(command().build(), SESSION);

      assertThat(count).isEqualTo(1);
      verify(datasetService).createDatasets(any(), any());
    }

    @Test
    void givenNoCallbackUrl_whenImportingFile_thenImportSucceedsWithoutWebhook()
        throws IOException {
      int count = importerService.importFile(command().callbackUrl(null).build(), SESSION);

      assertThat(count).isEqualTo(1);
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

    @Test
    void givenNoImportableSheets_whenImportingFile_thenThrowsWithCreationContext()
        throws IOException {
      when(datasetService.createDatasets(any(), any()))
          .thenThrow(new IOException("The workbook has no sheet FineBI can import"));

      assertThatThrownBy(() -> importerService.importFile(command().build(), SESSION))
          .isInstanceOf(IOException.class)
          .hasMessage(
              "FineBI dataset creation failed: The workbook has no sheet FineBI can import");

      verifyNoInteractions(synchronizationLinkRegistry);
    }

    @Test
    void givenMoreThanMaxSheets_whenImportingFile_thenThrowsBeforeUploading() throws IOException {
      when(fileDownloadService.download(any(), any()))
          .thenReturn(new DocSpaceRawFile("Report.xlsx", workbookWithSheets(51)));

      assertThatThrownBy(() -> importerService.importFile(command().build(), SESSION))
          .isInstanceOf(ImportRejectedException.class)
          .hasMessage("The workbook has 51 sheets; the import limit is 50")
          .asInstanceOf(
              org.assertj.core.api.InstanceOfAssertFactories.type(ImportRejectedException.class))
          .satisfies(
              e -> {
                assertThat(e.getCode()).isEqualTo("import.error.tooManySheets");
                assertThat(e.getParams()).containsEntry("count", 51).containsEntry("max", 50);
              });

      verifyNoInteractions(attachmentService, datasetService);
    }
  }
}

package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.SynchronizationService;
import com.asc.fr.docspace.application.port.input.transfer.SynchronizationCommand;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService.Cancellable;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineDatasetLocation;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceOutcome;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.Sheet;
import com.asc.fr.docspace.domain.common.spreadsheet.Spreadsheet;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
class DefaultSynchronizationServiceTest {
  private static final String DECISION_BASE = "http://localhost:37799/webroot/decision";
  private static final DocSpaceAccountCredentials ADMIN =
      new DocSpaceAccountCredentials("admin@example.com", "1", "hash");
  private static final FileSynchronizationRecord TRACKED =
      new FileSynchronizationRecord("1", "uuid-1", 1);

  @Mock private DocSpaceTenantService tenantService;
  @Mock private DocSpaceFileDownloadService fileDownloadService;
  @Mock private FineAttachmentService attachmentService;
  @Mock private FineDatasetService datasetService;
  @Mock private FineSessionFactory sessionFactory;
  @Mock private TaskSchedulerService scheduler;
  @Mock private SynchronizationEventPublisher eventPublisher;
  @Mock private SynchronizationLinkRegistry registry;

  private SynchronizationService service;

  private static byte[] workbook(String... sheetNames) {
    StringBuilder sheets = new StringBuilder();
    StringBuilder rels = new StringBuilder();
    Map<String, String> parts = new LinkedHashMap<>();
    for (int i = 0; i < sheetNames.length; i++) {
      String rid = "rId" + (i + 1);
      String part = "xl/worksheets/sheet" + (i + 1) + ".xml";
      sheets
          .append("<sheet name=\"")
          .append(sheetNames[i])
          .append("\" sheetId=\"")
          .append(i + 1)
          .append("\" r:id=\"")
          .append(rid)
          .append("\"/>");
      rels.append(
          "<Relationship Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet"
              + (i + 1)
              + ".xml\" Id=\""
              + rid
              + "\"/>");
      parts.put(
          part,
          "<?xml version=\"1.0\"?><worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>H"
              + i
              + "</t></is></c></row></sheetData></worksheet>");
    }

    Map<String, String> entries = new LinkedHashMap<>();
    entries.put(
        "xl/workbook.xml",
        "<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
            + sheets
            + "</sheets></workbook>");
    entries.put("xl/_rels/workbook.xml.rels", "<Relationships>" + rels + "</Relationships>");
    entries.putAll(parts);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return bytes.toByteArray();
  }

  @BeforeEach
  void setUp() throws IOException {
    lenient().when(tenantService.isConfigured()).thenReturn(true);
    lenient().when(tenantService.adminCredentials()).thenReturn(ADMIN);
    lenient().when(tenantService.docSpaceUrl()).thenReturn("https://docspace.example.com");
    lenient()
        .when(sessionFactory.generateSession(DECISION_BASE))
        .thenReturn(new FineSession(DECISION_BASE, "fine_auth_token=fresh"));
    lenient()
        .when(fileDownloadService.download(any(), any()))
        .thenReturn(new DocSpaceRawFile("Report.xlsx", workbook("Report")));
    lenient()
        .when(attachmentService.uploadAttachment(any(), any()))
        .thenReturn(new FineAttachment("attach-1"));
    lenient()
        .when(datasetService.locateDatasets(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<String> ids = invocation.getArgument(0);
              Map<String, FineDatasetLocation> located = new HashMap<>();
              if (ids != null)
                for (String id : ids) located.put(id, new FineDatasetLocation("folder-1", id));
              return located;
            });
    lenient()
        .when(datasetService.replaceDatasets(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              List<FineReplaceDatasetCommand> commands = invocation.getArgument(0);
              if (commands == null) return Collections.emptyList();
              List<FineReplaceOutcome> outcomes = new ArrayList<>(commands.size());
              for (FineReplaceDatasetCommand command : commands)
                outcomes.add(FineReplaceOutcome.replaced(command.getTableId(), command.getName()));
              return outcomes;
            });

    service =
        new DefaultSynchronizationService(
            tenantService,
            fileDownloadService,
            attachmentService,
            datasetService,
            sessionFactory,
            scheduler,
            eventPublisher,
            registry);
  }

  private static SynchronizationCommand commandFor(String fileId) {
    return SynchronizationCommand.builder().decisionBase(DECISION_BASE).fileId(fileId).build();
  }

  private void tracking(FileSynchronizationRecord record) {
    when(registry.findByFile(record.getFileId())).thenReturn(Collections.singletonList(record));
  }

  private SynchronizationCommand commandForTracked() {
    return commandFor(TRACKED.getFileId());
  }

  @Nested
  class WhenSchedulingProducesNoEffect {
    @Test
    void givenUnknownFileId_whenScheduling_thenDoesNothing() {
      when(registry.findByFile("unknown")).thenReturn(Collections.emptyList());

      service.schedule(commandFor("unknown"));

      verifyNoInteractions(scheduler, datasetService, eventPublisher);
    }

    @Test
    void givenTenantNotConfigured_whenScheduling_thenDoesNothing() {
      tracking(TRACKED);
      when(tenantService.isConfigured()).thenReturn(false);

      service.schedule(commandForTracked());

      verifyNoInteractions(scheduler, datasetService, eventPublisher);
    }
  }

  @Nested
  class WhenDatasetExists {
    @BeforeEach
    void runScheduledTaskImmediately() {
      when(scheduler.schedule(anyLong(), any()))
          .thenAnswer(
              invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return (Cancellable) () -> {};
              });
    }

    @Test
    void givenKnownFile_whenScheduling_thenReplacesDatasetAndNotifies() throws IOException {
      tracking(TRACKED);

      service.schedule(commandForTracked());

      ArgumentCaptor<List<FineReplaceDatasetCommand>> replace = ArgumentCaptor.forClass(List.class);
      verify(datasetService).replaceDatasets(replace.capture(), any(), any());
      assertThat(replace.getValue())
          .extracting(FineReplaceDatasetCommand::getTableId)
          .containsExactly("uuid-1");
      verify(datasetService).refreshDataset(any(), any());
      verify(eventPublisher).datasetUpdated("uuid-1");
    }

    @Test
    void givenDatasetMovedToAnotherFolder_whenScheduling_thenSyncsUsingItsCurrentFolder()
        throws IOException {
      tracking(TRACKED);

      when(datasetService.locateDatasets(any(), any()))
          .thenReturn(
              Collections.singletonMap(
                  "uuid-1", new FineDatasetLocation("moved-folder", "My Renamed Dataset")));

      service.schedule(commandForTracked());

      ArgumentCaptor<List<FineReplaceDatasetCommand>> replace = ArgumentCaptor.forClass(List.class);
      verify(datasetService).replaceDatasets(replace.capture(), any(), any());
      assertThat(replace.getValue().get(0).getFolderId()).isEqualTo("moved-folder");
      assertThat(replace.getValue().get(0).getName()).isEqualTo("My Renamed Dataset");

      ArgumentCaptor<FineRefreshDatasetCommand> refresh =
          ArgumentCaptor.forClass(FineRefreshDatasetCommand.class);
      verify(datasetService).refreshDataset(refresh.capture(), any());
      assertThat(refresh.getValue().getFolderId()).isEqualTo("moved-folder");
      verify(eventPublisher).datasetUpdated("My Renamed Dataset");
      verify(registry, never()).remove(any());
    }

    @Test
    void givenDatasetNotFoundInFineBi_whenScheduling_thenLeavesMappingUntouched()
        throws IOException {
      tracking(TRACKED);

      when(datasetService.locateDatasets(any(), any())).thenReturn(Collections.emptyMap());

      service.schedule(commandForTracked());

      verify(datasetService, never()).replaceDatasets(any(), any(), any());
      verify(registry, never()).remove(any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenDatasetDeletedInFineBi_whenScheduling_thenUnregistersWithoutRecreating()
        throws IOException {
      tracking(TRACKED);

      doReturn(Collections.singletonList(FineReplaceOutcome.absent("uuid-1")))
          .when(datasetService)
          .replaceDatasets(any(), any(), any());

      service.schedule(commandForTracked());

      verify(registry).remove("uuid-1");
      verify(datasetService, never()).refreshDataset(any(), any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenTransientReplaceFailure_whenScheduling_thenKeepsTrackingEntry() throws IOException {
      tracking(TRACKED);

      doReturn(Collections.singletonList(FineReplaceOutcome.failed("uuid-1", "replace failed")))
          .when(datasetService)
          .replaceDatasets(any(), any(), any());

      service.schedule(commandForTracked());

      verify(registry, never()).remove(any());
      verify(datasetService, never()).refreshDataset(any(), any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenEntryWithNoDatasetUuid_whenScheduling_thenSkipsWithoutNotifying() throws IOException {
      tracking(new FileSynchronizationRecord(TRACKED.getFileId(), "", 1));

      service.schedule(commandForTracked());

      verify(datasetService, never()).replaceDatasets(any(), any(), any());
      verify(attachmentService, never()).uploadAttachment(any(), any());
      verify(registry, never()).remove(any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenLegacyRowWithoutSheetId_whenScheduling_thenLeavesItUntouched() throws IOException {
      tracking(new FileSynchronizationRecord("1", "uuid-legacy"));

      service.schedule(commandForTracked());

      verify(datasetService, never()).replaceDatasets(any(), any(), any());
      verify(registry, never()).remove(any());
      verifyNoInteractions(eventPublisher);
    }
  }

  @Nested
  class WhenEventsAreBursty {
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final Cancellable first = mock(Cancellable.class);
    private final Cancellable second = mock(Cancellable.class);
    private final Cancellable third = mock(Cancellable.class);

    @BeforeEach
    void deferScheduledTasks() {
      Deque<Cancellable> handles = new ArrayDeque<>(Arrays.asList(first, second, third));
      when(scheduler.schedule(anyLong(), any()))
          .thenAnswer(
              invocation -> {
                scheduledTasks.add(invocation.getArgument(1, Runnable.class));
                return handles.poll();
              });
      tracking(TRACKED);
    }

    @Test
    void givenBurstOfEventsForSameFile_whenScheduling_thenDebouncesToSingleResync()
        throws IOException {
      service.schedule(commandForTracked());
      service.schedule(commandForTracked());
      service.schedule(commandForTracked());

      // Only the last-scheduled task survives; the superseded ones are cancelled.
      verify(first).cancel();
      verify(second).cancel();
      verify(third, never()).cancel();

      scheduledTasks.get(2).run();

      verify(datasetService).replaceDatasets(any(), any(), any());
      verify(eventPublisher, times(1)).datasetUpdated("uuid-1");
    }

    @Test
    void givenEventAfterDebounceWindow_whenScheduling_thenTriggersNewResync() throws IOException {
      service.schedule(commandForTracked());
      scheduledTasks.get(0).run();
      service.schedule(commandForTracked());
      scheduledTasks.get(1).run();

      verify(eventPublisher, times(2)).datasetUpdated("uuid-1");
    }
  }

  @Nested
  class WhenWorkbookHasMultipleSheets {
    @BeforeEach
    void runScheduledTaskImmediately() {
      when(scheduler.schedule(anyLong(), any()))
          .thenAnswer(
              invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return (Cancellable) () -> {};
              });
    }

    private void downloading(String... sheetNames) throws IOException {
      when(fileDownloadService.download(any(), any()))
          .thenReturn(new DocSpaceRawFile("Book.xlsx", workbook(sheetNames)));
    }

    @Test
    void givenOneRecordPerSheet_whenScheduling_thenReplacesEachAtItsSheetIndex()
        throws IOException {
      downloading("Sales", "Inventory");

      when(registry.findByFile("1"))
          .thenReturn(
              Arrays.asList(
                  new FileSynchronizationRecord("1", "uuid-sales", 1),
                  new FileSynchronizationRecord("1", "uuid-inv", 2)));

      service.schedule(commandFor("1"));

      ArgumentCaptor<List<FineReplaceDatasetCommand>> replace = ArgumentCaptor.forClass(List.class);
      verify(datasetService, times(2)).replaceDatasets(replace.capture(), any(), any());
      assertThat(replace.getAllValues())
          .extracting(list -> list.get(0))
          .extracting(
              FineReplaceDatasetCommand::getTableId, FineReplaceDatasetCommand::getSheetIndex)
          .containsExactly(
              org.assertj.core.groups.Tuple.tuple("uuid-sales", 0),
              org.assertj.core.groups.Tuple.tuple("uuid-inv", 1));

      verify(attachmentService, times(2)).uploadAttachment(any(), any());
      verify(eventPublisher).datasetUpdated("uuid-sales");
      verify(eventPublisher).datasetUpdated("uuid-inv");
      verify(registry, never()).remove(any());
    }

    @Test
    void givenOneSheetPreviewFails_whenScheduling_thenSkipsItAndStillSyncsTheRest()
        throws IOException {
      downloading("Sales", "Inventory");

      when(registry.findByFile("1"))
          .thenReturn(
              Arrays.asList(
                  new FileSynchronizationRecord("1", "uuid-sales", 1),
                  new FileSynchronizationRecord("1", "uuid-inv", 2)));
      doReturn(
              Arrays.asList(
                  FineReplaceOutcome.failed(
                      "uuid-sales", "FineBI sheet preview failed: FineWidgetNoDataException")))
          .doReturn(Arrays.asList(FineReplaceOutcome.replaced("uuid-inv", "uuid-inv")))
          .when(datasetService)
          .replaceDatasets(any(), any(), any());

      service.schedule(commandFor("1"));

      verify(datasetService, times(2)).replaceDatasets(any(), any(), any());
      verify(eventPublisher, never()).datasetUpdated("uuid-sales");
      verify(eventPublisher).datasetUpdated("uuid-inv");
      verify(registry, never()).remove(any());
    }

    @Test
    void givenSheetContentUnchanged_whenScheduling_thenSkipsFineBiCalls() throws IOException {
      downloading("Sales", "Inventory");

      List<Sheet> sheets = new Spreadsheet("Book.xlsx", workbook("Sales", "Inventory")).sheets();
      String salesHash = sheets.get(0).getContentHash();
      String invHash = sheets.get(1).getContentHash();

      when(registry.findByFile("1"))
          .thenReturn(
              Arrays.asList(
                  new FileSynchronizationRecord("1", "uuid-sales", 1, salesHash, 0L),
                  new FileSynchronizationRecord("1", "uuid-inv", 2, invHash, 0L)));

      service.schedule(commandFor("1"));

      verify(datasetService, never()).replaceDatasets(any(), any(), any());
      verify(attachmentService, never()).uploadAttachment(any(), any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenOnlyOneSheetChanged_whenScheduling_thenReplacesOnlyThatSheet() throws IOException {
      downloading("Sales", "Inventory");

      List<Sheet> sheets = new Spreadsheet("Book.xlsx", workbook("Sales", "Inventory")).sheets();

      when(registry.findByFile("1"))
          .thenReturn(
              Arrays.asList(
                  new FileSynchronizationRecord("1", "uuid-sales", 1, "stale-hash", 0L),
                  new FileSynchronizationRecord(
                      "1", "uuid-inv", 2, sheets.get(1).getContentHash(), 0L)));

      service.schedule(commandFor("1"));

      ArgumentCaptor<List<FineReplaceDatasetCommand>> replace = ArgumentCaptor.forClass(List.class);
      verify(datasetService, times(1)).replaceDatasets(replace.capture(), any(), any());
      assertThat(replace.getValue())
          .extracting(FineReplaceDatasetCommand::getTableId)
          .containsExactly("uuid-sales");
      verify(attachmentService, times(1)).uploadAttachment(any(), any());
      verify(eventPublisher).datasetUpdated("uuid-sales");
      verify(eventPublisher, never()).datasetUpdated("uuid-inv");
    }

    @Test
    void givenSheetRenamedButSameSheetId_whenScheduling_thenStillMatchesBySheetId()
        throws IOException {
      downloading("RenamedSales");

      when(registry.findByFile("1"))
          .thenReturn(
              Collections.singletonList(new FileSynchronizationRecord("1", "uuid-sales", 1)));

      service.schedule(commandFor("1"));

      ArgumentCaptor<List<FineReplaceDatasetCommand>> replace = ArgumentCaptor.forClass(List.class);
      verify(datasetService).replaceDatasets(replace.capture(), any(), any());
      assertThat(replace.getValue())
          .extracting(FineReplaceDatasetCommand::getSheetIndex)
          .containsExactly(0);
      verify(eventPublisher).datasetUpdated("uuid-sales");
      verify(registry, never()).remove(any());
    }

    @Test
    void givenSheetIdMissingFromWorkbook_whenScheduling_thenDropsMapping() throws IOException {
      downloading("Sales");

      when(registry.findByFile("1"))
          .thenReturn(
              Collections.singletonList(new FileSynchronizationRecord("1", "uuid-gone", 9)));

      service.schedule(commandFor("1"));

      verify(registry).remove("uuid-gone");
      verify(datasetService, never()).replaceDatasets(any(), any(), any());
      verify(attachmentService, never()).uploadAttachment(any(), any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenSheetRemovedFromWorkbook_whenScheduling_thenDropsItsMappingAndSyncsTheRest()
        throws IOException {
      downloading("Sales");

      when(registry.findByFile("1"))
          .thenReturn(
              Arrays.asList(
                  new FileSynchronizationRecord("1", "uuid-sales", 1),
                  new FileSynchronizationRecord("1", "uuid-gone", 5)));

      service.schedule(commandFor("1"));

      verify(registry).remove("uuid-gone");
      ArgumentCaptor<List<FineReplaceDatasetCommand>> replace = ArgumentCaptor.forClass(List.class);
      verify(datasetService).replaceDatasets(replace.capture(), any(), any());
      assertThat(replace.getValue())
          .extracting(FineReplaceDatasetCommand::getTableId)
          .containsExactly("uuid-sales");
      verify(eventPublisher).datasetUpdated("uuid-sales");
      verify(eventPublisher, never()).datasetUpdated("uuid-gone");
    }
  }
}

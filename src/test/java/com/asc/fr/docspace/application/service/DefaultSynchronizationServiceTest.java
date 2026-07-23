package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asc.fr.docspace.application.exception.DatasetAbsentException;
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
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
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
      new FileSynchronizationRecord("1", "Report", "folder-1", "uuid-1");

  @Mock private DocSpaceTenantService tenantService;
  @Mock private DocSpaceFileDownloadService fileDownloadService;
  @Mock private FineAttachmentService attachmentService;
  @Mock private FineDatasetService datasetService;
  @Mock private FineSessionFactory sessionFactory;
  @Mock private TaskSchedulerService scheduler;
  @Mock private SynchronizationEventPublisher eventPublisher;
  @Mock private SynchronizationLinkRegistry registry;

  private SynchronizationService service;

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
        .thenReturn(new DocSpaceRawFile("Report.xlsx", new byte[] {1, 2, 3}));
    lenient()
        .when(attachmentService.uploadAttachment(any(), any()))
        .thenReturn(new FineAttachment("attach-1"));

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

      ArgumentCaptor<FineReplaceDatasetCommand> replace =
          ArgumentCaptor.forClass(FineReplaceDatasetCommand.class);
      verify(datasetService).replaceDataset(replace.capture(), any(), any());
      assertThat(replace.getValue().getTableId()).isEqualTo("uuid-1");
      verify(datasetService).refreshDataset(any(), any());
      verify(eventPublisher).datasetUpdated("Report");
    }

    @Test
    void givenDatasetDeletedInFineBi_whenScheduling_thenUnregistersWithoutRecreating()
        throws IOException {
      tracking(TRACKED);
      doThrow(new DatasetAbsentException("uuid-1"))
          .when(datasetService)
          .replaceDataset(any(), any(), any());

      service.schedule(commandForTracked());

      verify(registry).remove("uuid-1");
      verify(datasetService, never()).refreshDataset(any(), any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenTransientReplaceFailure_whenScheduling_thenKeepsTrackingEntry() throws IOException {
      tracking(TRACKED);
      doThrow(new IOException("replace failed"))
          .when(datasetService)
          .replaceDataset(any(), any(), any());

      service.schedule(commandForTracked());

      verify(registry, never()).remove(any());
      verify(datasetService, never()).refreshDataset(any(), any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenEntryWithNoDatasetUuid_whenScheduling_thenSkipsWithoutNotifying() throws IOException {
      tracking(new FileSynchronizationRecord(TRACKED.getFileId(), "Report", "folder-7", ""));

      service.schedule(commandForTracked());

      verify(datasetService, never()).replaceDataset(any(), any(), any());
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

      verify(datasetService).replaceDataset(any(), any(), any());
      verify(eventPublisher, times(1)).datasetUpdated("Report");
    }

    @Test
    void givenEventAfterDebounceWindow_whenScheduling_thenTriggersNewResync() throws IOException {
      service.schedule(commandForTracked());
      scheduledTasks.get(0).run();
      service.schedule(commandForTracked());
      scheduledTasks.get(1).run();

      verify(eventPublisher, times(2)).datasetUpdated("Report");
    }
  }
}

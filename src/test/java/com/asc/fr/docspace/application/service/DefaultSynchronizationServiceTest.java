package com.asc.fr.docspace.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.application.port.input.SynchronizationService;
import com.asc.fr.docspace.application.port.input.transfer.SynchronizationCommand;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultSynchronizationServiceTest {
  private static final String DECISION_BASE = "http://localhost:37799/webroot/decision";

  private TestPorts.InMemorySynchronizationService synchronizationService;
  private TestPorts.InMemoryDocSpaceTenantService tenantService;
  private TestPorts.RecordingFineService recordingFineService;
  private TestPorts.RecordingPublisher eventPublisher;
  private TestPorts.StubFileStorage fileStorage;
  private SynchronizationService service;

  @BeforeEach
  void setUp() {
    tenantService = new TestPorts.InMemoryDocSpaceTenantService();
    tenantService.config =
        new DocSpaceTenantConfiguration(
            "https://docspace.example.com",
            new DocSpaceAccountCredentials("admin@example.com", "a1", "hash"));
    fileStorage = new TestPorts.StubFileStorage();
    recordingFineService = new TestPorts.RecordingFineService();
    synchronizationService = new TestPorts.InMemorySynchronizationService();
    eventPublisher = new TestPorts.RecordingPublisher();

    service =
        new DefaultSynchronizationService(
            new DefaultDocSpaceTenantService(tenantService),
            fileStorage,
            recordingFineService,
            recordingFineService,
            new TestPorts.StubSessionFactory(),
            new TestPorts.ImmediateTaskSchedulerService(),
            eventPublisher,
            synchronizationService);
  }

  private SynchronizationCommand commandFor(String fileId) {
    return SynchronizationCommand.builder().decisionBase(DECISION_BASE).fileId(fileId).build();
  }

  @Nested
  class WhenSchedulingProducesNoEffect {
    @Test
    void givenUnknownFileId_whenScheduling_thenDoesNothing() {
      service.schedule(commandFor("unknown"));

      assertTrue(recordingFineService.noCalls());
      assertTrue(eventPublisher.updated.isEmpty());
    }

    @Test
    void givenTenantNotConfigured_whenScheduling_thenDoesNothing() throws IOException {
      synchronizationService.put(
          new FileSynchronizationRecord("22", "Report", "folder-7", "uuid-1"));
      tenantService.config = DocSpaceTenantConfiguration.empty();

      service.schedule(commandFor("22"));

      assertTrue(recordingFineService.noCalls());
    }
  }

  @Nested
  class WhenDatasetExists {
    @Test
    void givenKnownFile_whenScheduling_thenReplacesDatasetAndNotifies() throws IOException {
      synchronizationService.put(
          new FileSynchronizationRecord("22", "Report", "folder-7", "uuid-1"));

      service.schedule(commandFor("22"));

      assertEquals(1, recordingFineService.replaceDatasetCalls.size());
      assertEquals("uuid-1", recordingFineService.replaceDatasetCalls.get(0).getTableId());
      assertEquals(1, recordingFineService.refreshDatasetCalls.size());
      assertEquals("uuid-1", recordingFineService.refreshDatasetCalls.get(0).getTableId());
      assertEquals("uuid-1", synchronizationService.find("22").getTableId());
      assertEquals("Report", eventPublisher.updated.get(0));
    }

    @Test
    void givenDatasetDeletedInFineBi_whenScheduling_thenUnregistersWithoutRecreating()
        throws IOException {
      synchronizationService.put(
          new FileSynchronizationRecord("22", "Report", "folder-7", "uuid-1"));
      recordingFineService.datasetAbsent = true;

      service.schedule(commandFor("22"));

      assertNull(synchronizationService.find("22"));
      assertTrue(recordingFineService.createDatasetCalls.isEmpty());
      assertTrue(eventPublisher.updated.isEmpty());
    }

    @Test
    void givenTransientReplaceFailure_whenScheduling_thenKeepsTrackingEntry() throws IOException {
      synchronizationService.put(
          new FileSynchronizationRecord("22", "Report", "folder-7", "uuid-1"));
      recordingFineService.failReplace = true;

      service.schedule(commandFor("22"));

      assertEquals("uuid-1", synchronizationService.find("22").getTableId());
      assertTrue(recordingFineService.createDatasetCalls.isEmpty());
      assertTrue(eventPublisher.updated.isEmpty());
    }

    @Test
    void givenEntryWithNoDatasetUuid_whenScheduling_thenUnregistersEntry() throws IOException {
      synchronizationService.put(new FileSynchronizationRecord("22", "Report", "folder-7", ""));

      service.schedule(commandFor("22"));

      assertNull(synchronizationService.find("22"));
      assertTrue(eventPublisher.updated.isEmpty());
    }
  }

  @Nested
  class WhenEventsAreBursty {
    private TestPorts.DeferredTaskSchedulerService scheduler;

    @BeforeEach
    void setUpDeferredScheduler() {
      scheduler = new TestPorts.DeferredTaskSchedulerService();
      service =
          new DefaultSynchronizationService(
              new DefaultDocSpaceTenantService(tenantService),
              fileStorage,
              recordingFineService,
              recordingFineService,
              new TestPorts.StubSessionFactory(),
              scheduler,
              eventPublisher,
              synchronizationService);
    }

    @Test
    void givenBurstOfEventsForSameFile_whenScheduling_thenDebouncesToSingleResync()
        throws IOException {
      synchronizationService.put(
          new FileSynchronizationRecord("22", "Report", "folder-7", "uuid-1"));

      service.schedule(commandFor("22"));
      service.schedule(commandFor("22"));
      service.schedule(commandFor("22"));
      scheduler.flush();

      assertEquals(1, recordingFineService.replaceDatasetCalls.size());
      assertEquals("uuid-1", recordingFineService.replaceDatasetCalls.get(0).getTableId());
      assertEquals(1, eventPublisher.updated.size());
    }

    @Test
    void givenEventAfterDebounceWindow_whenScheduling_thenTriggersNewResync() throws IOException {
      synchronizationService.put(
          new FileSynchronizationRecord("22", "Report", "folder-7", "uuid-1"));

      service.schedule(commandFor("22"));
      scheduler.flush();
      service.schedule(commandFor("22"));
      scheduler.flush();

      assertEquals(2, eventPublisher.updated.size());
    }
  }
}

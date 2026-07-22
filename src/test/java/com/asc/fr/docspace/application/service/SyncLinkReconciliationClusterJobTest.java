package com.asc.fr.docspace.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.application.job.SyncLinkJobSchedule;
import com.asc.fr.docspace.application.job.SyncLinkReconciliationClusterJob;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncLinkReconciliationClusterJobTest {
  private static final String DECISION_BASE = "http://localhost:37799/webroot/decision";

  private TestPorts.InMemorySynchronizationService synchronizationService;
  private TestPorts.RecordingDocSpaceFileRetrievalService retrievalService;
  private TestPorts.InMemoryDocSpaceTenantService tenantService;
  private TestPorts.RecordingFineService fineService;
  private SyncLinkReconciliationClusterJob job;

  @BeforeEach
  void setUp() {
    tenantService = new TestPorts.InMemoryDocSpaceTenantService();
    tenantService.config =
        new DocSpaceTenantConfiguration(
            "https://docspace.example.com",
            new DocSpaceAccountCredentials("admin@example.com", "a1", "hash"));
    synchronizationService = new TestPorts.InMemorySynchronizationService();
    synchronizationService.decisionBase = DECISION_BASE;
    retrievalService = new TestPorts.RecordingDocSpaceFileRetrievalService();
    fineService = new TestPorts.RecordingFineService();
    job =
        new SyncLinkReconciliationClusterJob(
            synchronizationService,
            synchronizationService,
            retrievalService,
            new DefaultDocSpaceTenantService(tenantService),
            fineService,
            new TestPorts.StubSessionFactory(),
            new SyncLinkJobSchedule(120000, 3600000, 86400000));
  }

  private void track(String fileId, String tableId) throws IOException {
    synchronizationService.put(
        new FileSynchronizationRecord(fileId, "Report", "folder-1", tableId));
  }

  @Test
  void givenTenantNotConfigured_whenRunning_thenProbesNothing() throws IOException {
    tenantService.config = DocSpaceTenantConfiguration.empty();
    track("22", "uuid-1");

    job.run();

    assertTrue(retrievalService.existsProbes.isEmpty());
    assertTrue(fineService.datasetExistsProbes.isEmpty());
    assertNotNull(synchronizationService.find("22"));
  }

  @Test
  void givenNoDecisionBase_whenRunning_thenProbesNothing() throws IOException {
    synchronizationService.decisionBase = "";
    track("22", "uuid-1");

    job.run();

    assertTrue(retrievalService.existsProbes.isEmpty());
    assertNotNull(synchronizationService.find("22"));
  }

  @Test
  void givenDocSpaceFileDeleted_whenRunning_thenRemovesLinkWithoutProbingFineBi()
      throws IOException {
    track("22", "uuid-1");
    retrievalService.absentFileIds.add("22");

    job.run();

    assertNull(synchronizationService.find("22"));
    assertTrue(fineService.datasetExistsProbes.isEmpty());
  }

  @Test
  void givenFineBiDatasetDeleted_whenRunning_thenRemovesLink() throws IOException {
    track("22", "uuid-gone");
    fineService.absentTableIds.add("uuid-gone");

    job.run();

    assertNull(synchronizationService.find("22"));
    assertEquals(1, fineService.datasetExistsProbes.size());
  }

  @Test
  void givenBothSidesAlive_whenRunning_thenKeepsAndProbesBoth() throws IOException {
    track("22", "uuid-live");

    job.run();

    assertNotNull(synchronizationService.find("22"));
    assertEquals(1, retrievalService.existsProbes.size());
    assertEquals(1, fineService.datasetExistsProbes.size());
  }

  @Test
  void givenProbeFails_whenRunning_thenKeepsTheLink() throws IOException {
    track("22", "uuid-1");
    retrievalService.failWith = new IOException("docspace unreachable");

    job.run();

    assertNotNull(synchronizationService.find("22"));
  }

  @Test
  void givenMoreLinksThanOneRunCap_whenRunning_thenPagesThroughAndStopsAtTheCap()
      throws IOException {
    for (int i = 0; i < 250; i++) track(String.format("%04d", i), "uuid-" + i);

    job.run();

    assertEquals(100, retrievalService.existsProbes.size());
    assertEquals(100, fineService.datasetExistsProbes.size());
  }

  @Test
  void givenSchedule_thenIsClusteredWithNameAndInterval() {
    assertEquals("link-cluster-reconciliation", job.name());
    assertTrue(job.periodMillis() > 0);
    assertTrue(job.initialDelayMillis() >= 0);
  }
}

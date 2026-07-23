package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asc.fr.docspace.application.job.SyncLinkJobSchedule;
import com.asc.fr.docspace.application.job.SyncLinkReconciliationClusterJob;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileRetrievalService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncLinkReconciliationClusterJobTest {
  private static final String DECISION_BASE = "http://localhost:37799/webroot/decision";
  private static final DocSpaceAccountCredentials ADMIN =
      new DocSpaceAccountCredentials("admin@example.com", "1", "hash");

  @Mock private SynchronizationLinkRegistry registry;
  @Mock private SynchronizationSettings settings;
  @Mock private DocSpaceFileRetrievalService docSpaceFiles;
  @Mock private DocSpaceTenantService tenantService;
  @Mock private FineDatasetService datasetService;
  @Mock private FineSessionFactory sessionFactory;

  private SyncLinkReconciliationClusterJob job;

  @BeforeEach
  void setUp() {
    lenient().when(tenantService.isConfigured()).thenReturn(true);
    lenient().when(tenantService.docSpaceUrl()).thenReturn("https://docspace.example.com");
    lenient().when(tenantService.adminCredentials()).thenReturn(ADMIN);
    lenient().when(settings.loadDecisionBase()).thenReturn(DECISION_BASE);
    lenient()
        .when(sessionFactory.generateSession(DECISION_BASE))
        .thenReturn(new FineSession(DECISION_BASE, "fine_auth_token=fresh"));
    job =
        new SyncLinkReconciliationClusterJob(
            registry,
            settings,
            docSpaceFiles,
            tenantService,
            datasetService,
            sessionFactory,
            new SyncLinkJobSchedule(120000, 3600000, 86400000));
  }

  private static FileSynchronizationRecord record(String fileId, String tableId) {
    return new FileSynchronizationRecord(fileId, "Report", "folder-1", tableId);
  }

  private void staleLinks(FileSynchronizationRecord... records) {
    when(registry.staleLinks(anyLong(), any(), anyInt())).thenReturn(Arrays.asList(records));
  }

  @Test
  void givenTenantNotConfigured_whenRunning_thenProbesNothing() {
    when(tenantService.isConfigured()).thenReturn(false);

    job.run();

    verifyNoInteractions(registry, docSpaceFiles, datasetService);
  }

  @Test
  void givenNoDecisionBase_whenRunning_thenProbesNothing() {
    when(settings.loadDecisionBase()).thenReturn("");

    job.run();

    verifyNoInteractions(registry, docSpaceFiles, datasetService);
  }

  @Test
  void givenDocSpaceFileDeleted_whenRunning_thenRemovesLinkWithoutProbingFineBi()
      throws IOException {
    staleLinks(record("1", "uuid-1"));
    when(docSpaceFiles.fileExists(any(), eq("1"), any())).thenReturn(false);

    job.run();

    verify(registry).remove("uuid-1");
    verify(datasetService, never()).datasetExists(any(), any(), any());
  }

  @Test
  void givenFineBiDatasetDeleted_whenRunning_thenRemovesLink() throws IOException {
    staleLinks(record("1", "uuid-gone"));
    when(docSpaceFiles.fileExists(any(), eq("1"), any())).thenReturn(true);
    when(datasetService.datasetExists(eq("uuid-gone"), any(), any())).thenReturn(false);

    job.run();

    verify(registry).remove("uuid-gone");
    verify(datasetService).datasetExists(eq("uuid-gone"), any(), any());
  }

  @Test
  void givenBothSidesAlive_whenRunning_thenKeepsAndProbesBoth() throws IOException {
    FileSynchronizationRecord link = record("1", "uuid-live");
    staleLinks(link);
    when(docSpaceFiles.fileExists(any(), eq("1"), any())).thenReturn(true);
    when(datasetService.datasetExists(eq("uuid-live"), any(), any())).thenReturn(true);

    job.run();

    verify(docSpaceFiles).fileExists(any(), eq("1"), any());
    verify(datasetService).datasetExists(eq("uuid-live"), any(), any());
    verify(registry).put(link);
    verify(registry, never()).remove(any());
  }

  @Test
  void givenProbeFails_whenRunning_thenKeepsTheLink() throws IOException {
    staleLinks(record("1", "uuid-1"));
    when(docSpaceFiles.fileExists(any(), eq("1"), any()))
        .thenThrow(new IOException("docspace unreachable"));

    job.run();

    verify(registry, never()).remove(any());
    verify(registry, never()).put(any());
  }

  @Test
  void givenMoreLinksThanOneRunCap_whenRunning_thenStopsAtTheCap() throws IOException {
    List<FileSynchronizationRecord> page = new ArrayList<>();
    for (int i = 0; i < 250; i++) page.add(record("file-" + i, "uuid-" + i));
    when(registry.staleLinks(anyLong(), any(), anyInt())).thenReturn(page);
    when(docSpaceFiles.fileExists(any(), any(), any())).thenReturn(true);
    when(datasetService.datasetExists(any(), any(), any())).thenReturn(true);

    job.run();

    verify(docSpaceFiles, times(100)).fileExists(any(), any(), any());
    verify(datasetService, times(100)).datasetExists(any(), any(), any());
  }

  @Test
  void givenSchedule_thenIsClusteredWithNameAndInterval() {
    assertThat(job.name()).isEqualTo("link-cluster-reconciliation");
    assertThat(job.periodMillis()).isPositive();
    assertThat(job.initialDelayMillis()).isNotNegative();
  }

  @Test
  void givenNoStaleLinks_whenRunning_thenDoesNothing() throws IOException {
    when(registry.staleLinks(anyLong(), any(), anyInt())).thenReturn(Collections.emptyList());

    job.run();

    verifyNoInteractions(docSpaceFiles, datasetService);
    verify(registry, never()).remove(any());
  }
}

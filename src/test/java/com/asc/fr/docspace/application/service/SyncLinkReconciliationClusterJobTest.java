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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

  @Captor private ArgumentCaptor<Collection<String>> fileIdsCaptor;

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
            new SyncLinkJobSchedule(120000, 3600000, 86400000, 100));
  }

  private static FileSynchronizationRecord record(String fileId, String tableId) {
    return record(fileId, tableId, "folder-1");
  }

  private static FileSynchronizationRecord record(String fileId, String tableId, String folderId) {
    return new FileSynchronizationRecord(fileId, "Report", folderId, tableId);
  }

  private void staleLinks(FileSynchronizationRecord... records) {
    when(registry.staleLinks(anyLong(), any(), anyInt())).thenReturn(Arrays.asList(records));
  }

  private void docSpaceProbes(Function<String, CompletableFuture<Boolean>> perId)
      throws IOException {
    when(docSpaceFiles.fileExistenceProbes(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<String> ids = invocation.getArgument(1);
              Map<String, CompletableFuture<Boolean>> probes = new HashMap<>();
              for (String id : ids) probes.put(id, perId.apply(id));
              return probes;
            });
  }

  private void fineBiHolds(String... tableIds) {
    Set<String> ids = new HashSet<>(Arrays.asList(tableIds));
    when(datasetService.tableIdsInFolderAsync(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(ids));
  }

  private static CompletableFuture<Boolean> failed() {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    future.completeExceptionally(new IOException("docspace unreachable"));
    return future;
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
    docSpaceProbes(id -> CompletableFuture.completedFuture(false));

    job.run();

    verify(registry).remove("uuid-1");
    verify(datasetService, never()).tableIdsInFolderAsync(any(), any());
  }

  @Test
  void givenFineBiDatasetDeleted_whenRunning_thenRemovesLink() throws IOException {
    staleLinks(record("1", "uuid-gone"));
    docSpaceProbes(id -> CompletableFuture.completedFuture(true));
    fineBiHolds("some-other-uuid");

    job.run();

    verify(registry).remove("uuid-gone");
    verify(datasetService).tableIdsInFolderAsync(eq("folder-1"), any());
  }

  @Test
  void givenBothSidesAlive_whenRunning_thenKeepsAndProbesBoth() throws IOException {
    FileSynchronizationRecord link = record("1", "uuid-live");
    staleLinks(link);
    docSpaceProbes(id -> CompletableFuture.completedFuture(true));
    fineBiHolds("uuid-live");

    job.run();

    verify(docSpaceFiles).fileExistenceProbes(any(), any(), any());
    verify(datasetService).tableIdsInFolderAsync(eq("folder-1"), any());
    verify(registry).put(link);
    verify(registry, never()).remove(any());
  }

  @Test
  void givenProbeFails_whenRunning_thenKeepsTheLink() throws IOException {
    staleLinks(record("1", "uuid-1"));
    docSpaceProbes(id -> failed());

    job.run();

    verify(registry, never()).remove(any());
    verify(registry, never()).put(any());
  }

  @Test
  void givenManyRecordsInOneFolder_whenRunning_thenProbesFineBiOncePerFolder() throws IOException {
    staleLinks(
        record("file-a", "uuid-a", "folder-1"),
        record("file-b", "uuid-b", "folder-1"),
        record("file-c", "uuid-c", "folder-2"));
    docSpaceProbes(id -> CompletableFuture.completedFuture(true));
    fineBiHolds("uuid-a", "uuid-b", "uuid-c");

    job.run();

    verify(datasetService).tableIdsInFolderAsync(eq("folder-1"), any());
    verify(datasetService).tableIdsInFolderAsync(eq("folder-2"), any());
    verify(datasetService, times(2)).tableIdsInFolderAsync(any(), any());
  }

  @Test
  void givenMoreLinksThanOneRunCap_whenRunning_thenStopsAtTheCap() throws IOException {
    List<FileSynchronizationRecord> page = new ArrayList<>();
    for (int i = 0; i < 250; i++) page.add(record("file-" + i, "uuid-" + i));
    when(registry.staleLinks(anyLong(), any(), anyInt())).thenReturn(page);
    docSpaceProbes(id -> CompletableFuture.completedFuture(true));
    fineBiHolds();

    job.run();

    verify(docSpaceFiles).fileExistenceProbes(any(), fileIdsCaptor.capture(), any());
    assertThat(fileIdsCaptor.getValue()).hasSize(100);
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

package com.asc.fr.docspace.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.application.job.JobSchedule;
import com.asc.fr.docspace.application.job.WebhookReconciliationClusterJob;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebhookReconciliationClusterJobTest {
  private static final String CALLBACK = "https://fr.example.com/decision/url/webhook";

  private TestPorts.InMemorySynchronizationService synchronizationService;
  private TestPorts.InMemoryDocSpaceTenantService tenantService;
  private TestPorts.RecordingWebhookRegistrar webhookRegistrar;
  private WebhookReconciliationClusterJob job;

  @BeforeEach
  void setUp() {
    tenantService = new TestPorts.InMemoryDocSpaceTenantService();
    tenantService.config =
        new DocSpaceTenantConfiguration(
            "https://docspace.example.com",
            new DocSpaceAccountCredentials("admin@example.com", "1", "hash"));
    synchronizationService = new TestPorts.InMemorySynchronizationService();
    webhookRegistrar = new TestPorts.RecordingWebhookRegistrar();
    job =
        new WebhookReconciliationClusterJob(
            synchronizationService,
            new DefaultDocSpaceTenantService(tenantService),
            webhookRegistrar,
            new JobSchedule(5000, 300000));
  }

  @Test
  void givenConfiguredTenantWithCallback_whenRunning_thenReEnsuresWebhook() {
    synchronizationService.storeCallbackUrl(CALLBACK);

    job.run();

    assertTrue(webhookRegistrar.registered.contains(CALLBACK));
  }

  @Test
  void givenNoCallbackStored_whenRunning_thenDoesNothing() {
    job.run();

    assertTrue(webhookRegistrar.registered.isEmpty());
  }

  @Test
  void givenTenantNotConfigured_whenRunning_thenDoesNothing() {
    tenantService.config = DocSpaceTenantConfiguration.empty();
    synchronizationService.storeCallbackUrl(CALLBACK);

    job.run();

    assertTrue(webhookRegistrar.registered.isEmpty());
  }

  @Test
  void givenSchedule_thenExposesNameAndInterval() {
    assertEquals("webhook-cluster-reconciliation", job.name());
    assertTrue(job.periodMillis() > 0);
    assertTrue(job.initialDelayMillis() >= 0);
  }
}

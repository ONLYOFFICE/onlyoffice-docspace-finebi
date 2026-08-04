package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.asc.fr.docspace.application.job.JobSchedule;
import com.asc.fr.docspace.application.job.WebhookReconciliationClusterJob;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookReconciliationClusterJobTest {
  private static final String CALLBACK = "https://fr.example.com/decision/url/webhook";
  private static final String DOCSPACE_URL = "https://docspace.example.com";
  private static final String SAVED_URL = "https://other.example.com";
  private static final DocSpaceAccountCredentials ADMIN =
      new DocSpaceAccountCredentials("admin@example.com", "1", "hash");
  private static final DocSpaceAccountCredentials SAVED_ADMIN =
      new DocSpaceAccountCredentials("saved-admin@example.com", "2", "hash2");

  @Mock private SynchronizationSettings settings;
  @Mock private DocSpaceTenantService tenantService;
  @Mock private DocSpaceSavedTenantService savedTenantService;
  @Mock private WebhookRegistrar webhookRegistrar;

  private WebhookReconciliationClusterJob job;

  @BeforeEach
  void setUp() {
    job =
        new WebhookReconciliationClusterJob(
            settings,
            tenantService,
            savedTenantService,
            webhookRegistrar,
            new JobSchedule(5000, 300000));
  }

  @Nested
  class WhenTenantIsConfigured {
    @BeforeEach
    void configureTenant() {
      lenient().when(tenantService.isConfigured()).thenReturn(true);
      lenient().when(tenantService.docSpaceUrl()).thenReturn(DOCSPACE_URL);
      lenient().when(tenantService.adminCredentials()).thenReturn(ADMIN);
      lenient().when(savedTenantService.listConnections()).thenReturn(Collections.emptyList());
    }

    @Test
    void givenCallbackAndSecret_whenRunning_thenForceSyncsActiveTenantWebhook() {
      when(settings.loadCallbackUrl()).thenReturn(CALLBACK);
      when(settings.loadSecret()).thenReturn("TestSecret123");

      job.run();

      verify(webhookRegistrar)
          .ensureSynced(
              eq(new URL(DOCSPACE_URL)), eq(new URL(CALLBACK)), eq("TestSecret123"), eq(ADMIN));
      verify(webhookRegistrar, never()).ensureRegistered(any(), any(), any(), any());
    }

    @Test
    void givenNoCallbackStored_whenRunning_thenDoesNothing() {
      when(settings.loadCallbackUrl()).thenReturn("");

      job.run();

      verifyNoInteractions(webhookRegistrar);
    }

    @Test
    void givenNoActiveSecret_whenRunning_thenSkipsTheActiveTenant() {
      when(settings.loadCallbackUrl()).thenReturn(CALLBACK);
      when(settings.loadSecret()).thenReturn("");

      job.run();

      verifyNoInteractions(webhookRegistrar);
    }
  }

  @Test
  void givenActiveAndSavedTenants_whenRunning_thenForceSyncsOnlyTheActiveOne() {
    when(tenantService.isConfigured()).thenReturn(true);
    when(tenantService.docSpaceUrl()).thenReturn(DOCSPACE_URL);
    when(tenantService.adminCredentials()).thenReturn(ADMIN);
    when(settings.loadCallbackUrl()).thenReturn(CALLBACK);
    when(settings.loadSecret()).thenReturn("ActiveSecret123");
    when(savedTenantService.listConnections())
        .thenReturn(
            Collections.singletonList(
                new DocSpaceSavedTenantConnection(
                    new DocSpaceTenantConfiguration(SAVED_URL, SAVED_ADMIN), "SavedSecret123")));

    job.run();

    verify(webhookRegistrar)
        .ensureSynced(
            eq(new URL(DOCSPACE_URL)), eq(new URL(CALLBACK)), eq("ActiveSecret123"), eq(ADMIN));
    verify(webhookRegistrar)
        .ensureRegistered(
            eq(new URL(SAVED_URL)), eq(new URL(CALLBACK)), eq("SavedSecret123"), eq(SAVED_ADMIN));
    verify(webhookRegistrar, never()).ensureSynced(eq(new URL(SAVED_URL)), any(), any(), any());
    verify(webhookRegistrar, never())
        .ensureRegistered(eq(new URL(DOCSPACE_URL)), any(), any(), any());
  }

  @Test
  void givenSavedTenantsWithSecrets_whenRunning_thenReEnsuresEachOnesWebhook() {
    when(tenantService.isConfigured()).thenReturn(false);
    when(settings.loadCallbackUrl()).thenReturn(CALLBACK);
    when(savedTenantService.listConnections())
        .thenReturn(
            Collections.singletonList(
                new DocSpaceSavedTenantConnection(
                    new DocSpaceTenantConfiguration(SAVED_URL, SAVED_ADMIN), "SavedSecret123")));

    job.run();

    verify(webhookRegistrar)
        .ensureRegistered(
            eq(new URL(SAVED_URL)), eq(new URL(CALLBACK)), eq("SavedSecret123"), eq(SAVED_ADMIN));
  }

  @Test
  void givenSavedTenantWithoutSecret_whenRunning_thenSkipsIt() {
    when(tenantService.isConfigured()).thenReturn(false);
    when(settings.loadCallbackUrl()).thenReturn(CALLBACK);
    when(savedTenantService.listConnections())
        .thenReturn(
            Collections.singletonList(
                new DocSpaceSavedTenantConnection(
                    new DocSpaceTenantConfiguration(SAVED_URL, SAVED_ADMIN), "")));

    job.run();

    verifyNoInteractions(webhookRegistrar);
  }

  @Test
  void givenNoCallbackStored_whenRunning_thenSkipsEverythingIncludingSavedTenants() {
    when(settings.loadCallbackUrl()).thenReturn("");

    job.run();

    verifyNoInteractions(webhookRegistrar, savedTenantService);
  }

  @Test
  void givenSchedule_thenExposesNameAndInterval() {
    assertThat(job.name()).isEqualTo("webhook-cluster-reconciliation");
    assertThat(job.periodMillis()).isPositive();
    assertThat(job.initialDelayMillis()).isNotNegative();
  }
}

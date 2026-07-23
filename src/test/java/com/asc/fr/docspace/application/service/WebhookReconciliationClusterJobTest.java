package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asc.fr.docspace.application.job.JobSchedule;
import com.asc.fr.docspace.application.job.WebhookReconciliationClusterJob;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
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
  private static final DocSpaceAccountCredentials ADMIN =
      new DocSpaceAccountCredentials("admin@example.com", "1", "hash");

  @Mock private SynchronizationSettings settings;
  @Mock private DocSpaceTenantService tenantService;
  @Mock private WebhookRegistrar webhookRegistrar;

  private WebhookReconciliationClusterJob job;

  @BeforeEach
  void setUp() {
    job =
        new WebhookReconciliationClusterJob(
            settings, tenantService, webhookRegistrar, new JobSchedule(5000, 300000));
  }

  @Nested
  class WhenTenantIsConfigured {
    @BeforeEach
    void configureTenant() {
      when(tenantService.isConfigured()).thenReturn(true);
      lenient().when(tenantService.docSpaceUrl()).thenReturn(DOCSPACE_URL);
      lenient().when(tenantService.adminCredentials()).thenReturn(ADMIN);
    }

    @Test
    void givenCallbackAndSecret_whenRunning_thenReEnsuresWebhook() {
      when(settings.loadCallbackUrl()).thenReturn(CALLBACK);
      when(settings.loadSecret()).thenReturn("TestSecret123");

      job.run();

      verify(webhookRegistrar)
          .ensureRegistered(
              eq(new URL(DOCSPACE_URL)), eq(new URL(CALLBACK)), eq("TestSecret123"), eq(ADMIN));
    }

    @Test
    void givenNoCallbackStored_whenRunning_thenDoesNothing() {
      when(settings.loadCallbackUrl()).thenReturn("");

      job.run();

      verifyNoInteractions(webhookRegistrar);
    }
  }

  @Test
  void givenTenantNotConfigured_whenRunning_thenDoesNothing() {
    when(tenantService.isConfigured()).thenReturn(false);

    job.run();

    verifyNoInteractions(webhookRegistrar, settings);
  }

  @Test
  void givenSchedule_thenExposesNameAndInterval() {
    assertThat(job.name()).isEqualTo("webhook-cluster-reconciliation");
    assertThat(job.periodMillis()).isPositive();
    assertThat(job.initialDelayMillis()).isNotNegative();
  }
}

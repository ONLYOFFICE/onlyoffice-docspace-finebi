package com.asc.fr.docspace.application.job;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class WebhookReconciliationClusterJob implements ScheduledClusterJob {
  private final SynchronizationSettings synchronizationService;
  private final DocSpaceTenantService tenantService;
  private final WebhookRegistrar webhookRegistrar;
  private final JobSchedule schedule;

  @Override
  public String name() {
    return "webhook-cluster-reconciliation";
  }

  @Override
  public long initialDelayMillis() {
    return schedule.initialDelayMillis;
  }

  @Override
  public long periodMillis() {
    return schedule.periodMillis;
  }

  @Override
  public void run() {
    if (!tenantService.isConfigured()) return;

    String callbackUrl = synchronizationService.loadCallbackUrl();
    if (callbackUrl.isEmpty()) return;

    String secret = synchronizationService.loadSecret();
    if (secret.isEmpty()) return;

    webhookRegistrar.ensureRegistered(
        new URL(tenantService.docSpaceUrl()),
        new URL(callbackUrl),
        secret,
        tenantService.adminCredentials());
  }
}

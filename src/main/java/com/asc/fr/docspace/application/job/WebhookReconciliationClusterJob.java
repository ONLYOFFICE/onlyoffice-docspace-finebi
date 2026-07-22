package com.asc.fr.docspace.application.job;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.SynchronizationService;
import com.asc.fr.docspace.domain.common.URL;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class WebhookReconciliationClusterJob implements ScheduledClusterJob {
  private static final long INITIAL_DELAY_MS = TimeUnit.SECONDS.toMillis(5);
  private static final long PERIOD_MS = TimeUnit.MINUTES.toMillis(5);

  private final SynchronizationService synchronizationService;
  private final DocSpaceTenantService tenantService;
  private final WebhookRegistrar webhookRegistrar;

  @Override
  public String name() {
    return "webhook-cluster-reconciliation";
  }

  @Override
  public long initialDelayMillis() {
    return INITIAL_DELAY_MS;
  }

  @Override
  public long periodMillis() {
    return PERIOD_MS;
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

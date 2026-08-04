package com.asc.fr.docspace.application.job;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class WebhookReconciliationClusterJob implements ScheduledClusterJob {
  private final SynchronizationSettings synchronizationService;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceSavedTenantService savedTenantService;
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
    String callbackUrl = synchronizationService.loadCallbackUrl();
    if (callbackUrl.isEmpty()) return;
    URL callback = new URL(callbackUrl);

    String activeUrl = "";
    // Active tenant only: blind-overwrite FineBI's secret onto DocSpace (DocSpace never returns
    // the stored key, so there is no drift check — PUT every pass keeps signatures verifiable).
    if (tenantService.isConfigured()) {
      activeUrl = tenantService.docSpaceUrl();
      String secret = synchronizationService.loadSecret();
      if (!secret.isEmpty())
        webhookRegistrar.ensureSynced(
            new URL(activeUrl), callback, secret, tenantService.adminCredentials());
    }

    // Saved/background tenants: create-if-missing only — never force-overwrite their secrets.
    for (DocSpaceSavedTenantConnection saved : savedTenantService.listConnections()) {
      if (saved.getWebhookSecret().isEmpty()) continue;
      String savedUrl = saved.getConfiguration().getUrl().getValue();
      if (!activeUrl.isEmpty() && activeUrl.equals(savedUrl)) continue;
      webhookRegistrar.ensureRegistered(
          saved.getConfiguration().getUrl(),
          callback,
          saved.getWebhookSecret(),
          saved.getConfiguration().getAdmin());
    }
  }
}

package com.asc.fr.docspace;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.common.URL;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * On injector creation: if a tenant + callback are already stored, make sure the DocSpace webhook
 * still exists (non-blocking).
 */
@Singleton
class DocSpacePluginWebhookStartupRunner {
  @Inject
  DocSpacePluginWebhookStartupRunner(
      DocSpaceTenantService tenant,
      com.asc.fr.docspace.domain.SynchronizationService registry,
      WebhookRegistrar webhooks,
      TaskSchedulerService tasks) {
    tasks.run(
        () -> {
          if (!tenant.isConfigured()) return;

          String callbackUrl = registry.loadCallbackUrl();
          if (callbackUrl.isEmpty()) return;

          String secret = registry.loadSecret();
          if (secret.isEmpty()) return;

          webhooks.ensureRegistered(
              new URL(tenant.docSpaceUrl()),
              new URL(callbackUrl),
              secret,
              tenant.adminCredentials());
        });
  }
}

package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookRegisteredResponse;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * Admin-only endpoint that synchronously registers (or re-registers) the DocSpace webhook and
 * returns the outcome as JSON, so failures are visible rather than swallowed by a background
 * thread.
 */
public class WebhookRegisterHttpHandler extends JsonHttpHandler {
  private final WebhookRegistrar webhooks;
  private final DocSpaceTenantService tenant;
  private final SynchronizationSettings synchronizationSettings;

  @Inject
  public WebhookRegisterHttpHandler(
      DocSpaceTenantService tenant,
      SynchronizationSettings synchronizationSettings,
      WebhookRegistrar webhooks) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.webhookRegister);
    this.tenant = tenant;
    this.synchronizationSettings = synchronizationSettings;
    this.webhooks = webhooks;
  }

  @Override
  protected Object handleJson(HttpServletRequest request) throws Exception {
    requireAdmin(request, "Admin only.");
    if (!tenant.isConfigured())
      throw new PluginStatusException(400, "DocSpace is not configured yet.");

    DocSpaceAccountCredentials credentials = tenant.adminCredentials();
    if (!credentials.isComplete())
      throw new PluginStatusException(400, "Admin credentials are missing. Re-run setup.");

    String callbackUrl = ImportRoutes.webhookCallbackUrl(request);
    synchronizationSettings.storeCallbackUrl(callbackUrl);

    try {
      webhooks.register(
          new URL(tenant.docSpaceUrl()),
          new URL(callbackUrl),
          synchronizationSettings.ensureSecret(),
          credentials);
    } catch (IOException e) {
      throw new BadRequestStatusException(
          "The DocSpace webhook could not be registered automatically. Check DocSpace -"
              + "Settings - Webhooks for a conflicting entry, then try again.");
    }

    return new WebhookRegisteredResponse(callbackUrl);
  }
}

package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.adapters.input.web.RequestOrigin;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.imports.handler.ImportRoutes;
import com.asc.fr.docspace.adapters.input.web.tenant.transfer.CredentialsRequest;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceOriginService;
import com.asc.fr.docspace.application.port.input.DocSpaceOriginService.OriginCheck;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * Persists DocSpace URL and admin login (SDK username/password hash). Only FineBI admins can access
 * this endpoint. Inputs arrive as a JSON body — see {@link CredentialsRequest}.
 */
public class SetupHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantAdminService tenantAdminService;
  private final DocSpaceUserAccountService userAccountService;
  private final SynchronizationSettings synchronizationService;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceOriginService originService;
  private final WebhookRegistrar webhookRegistrar;

  @Inject
  public SetupHttpHandler(
      DocSpaceTenantAdminService tenantAdminService,
      DocSpaceUserAccountService userAccountService,
      SynchronizationSettings synchronizationService,
      DocSpaceTenantService tenantService,
      DocSpaceOriginService originService,
      WebhookRegistrar webhookRegistrar) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.setup);
    this.tenantAdminService = tenantAdminService;
    this.userAccountService = userAccountService;
    this.synchronizationService = synchronizationService;
    this.tenantService = tenantService;
    this.originService = originService;
    this.webhookRegistrar = webhookRegistrar;
  }

  static String cspError(String fineBiOrigin) {
    return "Add "
        + fineBiOrigin
        + " under DocSpace → Settings → Developer Tools → JavaScript SDK "
        + "(Allowed origins for API/CORS and embed CSP domains), then try again.";
  }

  private static DocSpaceAccountCredentials credentials(CredentialsRequest body) {
    try {
      return new DocSpaceAccountCredentials(body.getEmail(), body.getUserId(), body.getHash());
    } catch (InvalidCredentialsException e) {
      throw new BadRequestStatusException(
          "Sign in to DocSpace with your admin email and password before saving.");
    }
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    RequestUser user = requireAdmin(request, "Only FineBI administrators can configure DocSpace.");

    CredentialsRequest body = Requests.json(request, CredentialsRequest.class);
    if (!URL.isValid(body.getDocspaceUrl()))
      throw new BadRequestStatusException("Enter a valid DocSpace URL (http:// or https://).");

    URL docSpaceUrl = new URL(body.getDocspaceUrl());
    DocSpaceAccountCredentials credentials = credentials(body);

    String fineBiOrigin = RequestOrigin.of(request);
    if (originService.checkOrigin(docSpaceUrl, fineBiOrigin) == OriginCheck.BLOCKED)
      throw new BadRequestStatusException(cspError(fineBiOrigin));

    try {
      tenantAdminService.save(docSpaceUrl, credentials);
      userAccountService.saveLogin(user.name(), credentials);
    } catch (IOException e) {
      throw new BadRequestStatusException("Could not save settings: " + e.getMessage());
    }

    // Register the DocSpace webhook synchronously: automatic dataset syncing
    // depends on it, so a failure must surface a clear message rather than be
    // swallowed. The tenant config is already saved; re-running setup after
    // fixing the cause re-registers idempotently.
    try {
      String callbackUrl = ImportRoutes.webhookCallbackUrl(request);
      synchronizationService.storeCallbackUrl(callbackUrl);
      String secret = synchronizationService.ensureSecret();
      webhookRegistrar.register(
          new URL(tenantService.docSpaceUrl()),
          new URL(callbackUrl),
          secret,
          tenantService.adminCredentials());
    } catch (Exception e) {
      throw new BadRequestStatusException(
          "Settings were saved, but registering the DocSpace webhook failed: "
              + HttpJson.rootCause(e)
              + " Automatic dataset syncing will not work until this is resolved.");
    }

    return OkResponse.ok();
  }
}

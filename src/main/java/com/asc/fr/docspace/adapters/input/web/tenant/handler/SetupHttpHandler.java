package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.ErrorResponse;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.adapters.input.web.RequestOrigin;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.imports.handler.ImportRoutes;
import com.asc.fr.docspace.application.port.input.DocSpaceOriginService;
import com.asc.fr.docspace.application.port.input.DocSpaceOriginService.OriginCheck;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.domain.SynchronizationService;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.fr.decision.fun.impl.BaseHttpHandler;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Persists DocSpace URL and admin login (SDK username/password hash). Only FineBI admins can access
 * this endpoint.
 */
public class SetupHttpHandler extends BaseHttpHandler {
  private final DocSpaceTenantAdminService tenantAdminService;
  private final DocSpaceUserAccountService userAccountService;
  private final SynchronizationService synchronizationService;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceOriginService originService;
  private final WebhookRegistrar webhookRegistrar;
  private final PageRenderer pageRenderer;

  @Inject
  public SetupHttpHandler(
      DocSpaceTenantAdminService tenantAdminService,
      DocSpaceUserAccountService userAccountService,
      SynchronizationService synchronizationService,
      DocSpaceTenantService tenantService,
      DocSpaceOriginService originService,
      WebhookRegistrar webhookRegistrar,
      PageRenderer pageRenderer) {
    this.tenantAdminService = tenantAdminService;
    this.userAccountService = userAccountService;
    this.synchronizationService = synchronizationService;
    this.tenantService = tenantService;
    this.originService = originService;
    this.webhookRegistrar = webhookRegistrar;
    this.pageRenderer = pageRenderer;
  }

  static String cspError(String fineBiOrigin) {
    return "Add "
        + fineBiOrigin
        + " under DocSpace → Settings → Developer Tools → JavaScript SDK "
        + "(Allowed origins for API/CORS and embed CSP domains), then try again.";
  }

  private static boolean requiredJson(HttpServletRequest request) {
    String accept = request.getHeader("Accept");
    return accept != null && accept.contains("application/json");
  }

  private void respondError(HttpServletResponse response, HttpServletRequest request, String error)
      throws IOException {
    if (requiredJson(request)) {
      HttpJson.write(response, HttpServletResponse.SC_BAD_REQUEST, new ErrorResponse(error));
      return;
    }

    HttpJson.writeHtml(
        response, HttpServletResponse.SC_BAD_REQUEST, pageRenderer.renderError(error));
  }

  @Override
  public RequestMethod getMethod() {
    return RequestMethod.POST;
  }

  @Override
  public String getPath() {
    return PluginManifest.get().endpoints.setup;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  @ExecuteFunctionRecord
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    RequestUser user = RequestUser.from(request);
    if (!user.isAdmin()) {
      HttpJson.write(
          response,
          HttpServletResponse.SC_FORBIDDEN,
          new ErrorResponse("Only FineBI administrators can configure DocSpace."));
      return;
    }

    String rawDocSpaceUrl = request.getParameter("docspaceUrl");
    if (!URL.isValid(rawDocSpaceUrl)) {
      respondError(response, request, "Enter a valid DocSpace URL (http:// or https://).");
      return;
    }

    URL docSpaceUrl = new URL(rawDocSpaceUrl);
    DocSpaceAccountCredentials credentials;
    try {
      credentials =
          new DocSpaceAccountCredentials(
              request.getParameter("docspace_email"),
              request.getParameter("docspace_user_id"),
              request.getParameter("docspace_hash"));
    } catch (InvalidCredentialsException e) {
      respondError(
          response,
          request,
          "Sign in to DocSpace with your admin email and password before saving.");
      return;
    }

    String fineBiOrigin = RequestOrigin.of(request);
    if (originService.checkOrigin(docSpaceUrl, fineBiOrigin) == OriginCheck.BLOCKED) {
      respondError(response, request, cspError(fineBiOrigin));
      return;
    }

    try {
      tenantAdminService.save(docSpaceUrl, credentials);
      userAccountService.saveLogin(user.name(), credentials);
    } catch (IOException e) {
      respondError(response, request, "Could not save settings: " + e.getMessage());
      return;
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
      respondError(
          response,
          request,
          "Settings were saved, but registering the DocSpace webhook failed: "
              + HttpJson.rootCause(e)
              + " Automatic dataset syncing will not work until this is resolved.");
      return;
    }

    HttpJson.write(response, HttpServletResponse.SC_OK, OkResponse.ok());
  }
}

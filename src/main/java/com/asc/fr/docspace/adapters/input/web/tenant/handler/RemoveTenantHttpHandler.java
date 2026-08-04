package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.tenant.transfer.TenantUrlRequest;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.domain.common.URL;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * Removes one known DocSpace connection (saved credentials and sync links for that URL). When the
 * removed URL is the active tenant or the one the admin is signed into, user logins are cleared and
 * clients are notified to re-render.
 */
public class RemoveTenantHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantAdminService tenantAdminService;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceUserAccountService userAccountService;
  private final SynchronizationEventPublisher eventPublisher;

  @Inject
  public RemoveTenantHttpHandler(
      DocSpaceTenantAdminService tenantAdminService,
      DocSpaceTenantService tenantService,
      DocSpaceUserAccountService userAccountService,
      SynchronizationEventPublisher eventPublisher) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.removeTenant);
    this.tenantAdminService = tenantAdminService;
    this.tenantService = tenantService;
    this.userAccountService = userAccountService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    RequestUser admin =
        requireAdmin(
            request,
            "Only FineBI administrators can manage DocSpace tenants.",
            "client.error.admin.tenants");

    TenantUrlRequest body = Requests.json(request, TenantUrlRequest.class);
    if (!URL.isValid(body.getDocspaceUrl()))
      throw new BadRequestStatusException(
          "Enter a valid DocSpace URL (http:// or https://).", "client.error.url.invalid");

    URL docSpaceUrl = new URL(body.getDocspaceUrl());
    String target = docSpaceUrl.getValue();
    boolean removingActive = target.equals(tenantService.docSpaceUrl());
    boolean removingSignedIn = target.equals(userAccountService.signedInTenantUrl(admin.name()));

    try {
      if (removingActive || removingSignedIn) userAccountService.clearAll();
      tenantAdminService.removeTenant(docSpaceUrl);
      if (removingActive || removingSignedIn) eventPublisher.tenantReset();
    } catch (IOException e) {
      throw new PluginStatusException(
          500,
          "Could not remove DocSpace connection: " + HttpJson.rootCause(e),
          "client.error.tenant.removeFailed");
    }

    return OkResponse.ok();
  }
}

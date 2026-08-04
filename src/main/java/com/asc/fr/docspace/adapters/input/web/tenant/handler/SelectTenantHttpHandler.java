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
import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
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
 * Activates a previously saved DocSpace connection as the current tenant (admin only). Every stored
 * login is cleared — they were issued by the old tenant — except the acting admin's, which is moved
 * onto the new tenant by reusing that connection's stored admin credentials.
 */
public class SelectTenantHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantAdminService tenantAdminService;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceUserAccountService userAccountService;
  private final SynchronizationEventPublisher eventPublisher;

  @Inject
  public SelectTenantHttpHandler(
      DocSpaceTenantAdminService tenantAdminService,
      DocSpaceTenantService tenantService,
      DocSpaceUserAccountService userAccountService,
      SynchronizationEventPublisher eventPublisher) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.selectTenant);
    this.tenantAdminService = tenantAdminService;
    this.tenantService = tenantService;
    this.userAccountService = userAccountService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    RequestUser admin =
        requireAdmin(request, "Only FineBI administrators can manage DocSpace tenants.");

    TenantUrlRequest body = Requests.json(request, TenantUrlRequest.class);
    if (!URL.isValid(body.getDocspaceUrl()))
      throw new BadRequestStatusException("Enter a valid DocSpace URL (http:// or https://).");

    try {
      tenantAdminService.selectTenant(new URL(body.getDocspaceUrl()));
      // A tenant switch invalidates every stored authorization, not just the acting admin's:
      // every other login was issued by the old tenant. Clear first, then reuse this
      // connection's stored credentials to sign the acting admin straight into the new one, so
      // "signed in as" and the "Current" badge move with it without a fresh login form.
      userAccountService.clearAll();
      userAccountService.saveLogin(
          admin.name(), tenantService.adminCredentials(), tenantService.docSpaceUrl());
      eventPublisher.tenantReset();
    } catch (TenantLimitExceededException e) {
      throw new BadRequestStatusException(e.getMessage());
    } catch (IOException e) {
      throw new PluginStatusException(
          500, "Could not select DocSpace connection: " + HttpJson.rootCause(e));
    }

    return OkResponse.ok();
  }
}

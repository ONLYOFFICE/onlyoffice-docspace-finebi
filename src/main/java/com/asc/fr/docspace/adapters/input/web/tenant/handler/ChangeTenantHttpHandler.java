package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * Clears the current DocSpace connection so a different one can be configured (admin only) — unlike
 * {@link ResetHttpHandler}, this keeps every sync link: they simply go unused until the same
 * DocSpace files/tenant are reconnected, and only an explicit "Reset" is meant to wipe them.
 */
public class ChangeTenantHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantAdminService tenantAdminService;
  private final DocSpaceUserAccountService userAccountService;
  private final SynchronizationEventPublisher eventPublisher;

  @Inject
  public ChangeTenantHttpHandler(
      DocSpaceTenantAdminService tenantAdminService,
      DocSpaceUserAccountService userAccountService,
      SynchronizationEventPublisher eventPublisher) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.changeTenant);
    this.tenantAdminService = tenantAdminService;
    this.userAccountService = userAccountService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    requireAdmin(request, "Only FineBI administrators can change DocSpace tenant.");
    try {
      // Same ordering rationale as ResetHttpHandler: credentials go first, so a failure here
      // leaves the tenant configured and the admin simply retries.
      userAccountService.clearAll();
      tenantAdminService.changeTenant();
      // Push to every open plugin page so active users drop their
      // DocSpace session and re-render immediately (see useTenantResetListener).
      eventPublisher.tenantReset();
    } catch (TenantLimitExceededException e) {
      throw new BadRequestStatusException(e.getMessage());
    } catch (IOException e) {
      throw new PluginStatusException(
          500, "Could not change tenant configuration: " + HttpJson.rootCause(e));
    }

    return OkResponse.ok();
  }
}

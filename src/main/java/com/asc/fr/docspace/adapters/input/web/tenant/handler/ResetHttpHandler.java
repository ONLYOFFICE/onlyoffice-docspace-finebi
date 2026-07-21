package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/** Clears tenant configuration (admin only). */
public class ResetHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantAdminService tenantAdminService;
  private final DocSpaceUserAccountService userAccountService;
  private final SynchronizationEventPublisher eventPublisher;

  @Inject
  public ResetHttpHandler(
      DocSpaceTenantAdminService tenantAdminService,
      DocSpaceUserAccountService userAccountService,
      SynchronizationEventPublisher eventPublisher) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.reset);
    this.tenantAdminService = tenantAdminService;
    this.userAccountService = userAccountService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    requireAdmin(request, "Only FineBI administrators can change DocSpace tenant.");
    try {
      tenantAdminService.reset();
      // A tenant change invalidates every stored authorization, not just
      // the acting admin's: all credentials were issued by the old tenant.
      userAccountService.clearAll();
      // Push to every open plugin page so active users drop their
      // DocSpace session and re-render immediately (see useTenantResetListener).
      eventPublisher.tenantReset();
    } catch (IOException e) {
      throw new PluginStatusException(
          500, "Could not reset tenant configuration: " + HttpJson.rootCause(e));
    }

    return OkResponse.ok();
  }
}

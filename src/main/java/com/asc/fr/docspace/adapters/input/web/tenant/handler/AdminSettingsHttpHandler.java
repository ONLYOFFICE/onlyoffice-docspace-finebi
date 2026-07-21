package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.PageSelectorService;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Serves the admin console, the page embedded as the DocSpace card in FineBI's System Management.
 */
public class AdminSettingsHttpHandler extends DocSpacePageHttpHandler {
  @Inject
  public AdminSettingsHttpHandler(
      DocSpaceUserAccountService userAccountService,
      DocSpaceTenantService tenantService,
      PageSelectorService pageSelector,
      PageRenderer pageRenderer) {
    super(userAccountService, tenantService, pageSelector, pageRenderer);
  }

  @Override
  public String getPath() {
    return PluginManifest.get().endpoints.docspaceAdmin;
  }

  @Override
  protected boolean console(HttpServletRequest request) {
    return true;
  }
}

package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.PageSelectorService;
import com.asc.fr.docspace.application.port.input.transfer.Page;
import com.asc.fr.docspace.application.port.input.transfer.PageSelectionCommand;
import com.fr.decision.fun.impl.BaseHttpHandler;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves the DocSpace page. Launcher, logout, setup, login, or embed depending on request and
 * state.
 */
public class DocSpacePageHttpHandler extends BaseHttpHandler {
  private final DocSpaceUserAccountService userAccountService;
  private final DocSpaceTenantService tenantService;
  private final PageSelectorService pageSelector;
  private final PageRenderer pageRenderer;

  @Inject
  public DocSpacePageHttpHandler(
      DocSpaceUserAccountService userAccountService,
      DocSpaceTenantService tenantService,
      PageSelectorService pageSelector,
      PageRenderer pageRenderer) {
    this.userAccountService = userAccountService;
    this.tenantService = tenantService;
    this.pageSelector = pageSelector;
    this.pageRenderer = pageRenderer;
  }

  private static boolean flag(HttpServletRequest request, String name) {
    return "1".equals(request.getParameter(name));
  }

  protected boolean console(HttpServletRequest request) {
    return flag(request, "setup");
  }

  @Override
  public RequestMethod getMethod() {
    return RequestMethod.GET;
  }

  @Override
  public String getPath() {
    return PluginManifest.get().endpoints.docspace;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  @ExecuteFunctionRecord
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    response.setHeader("Cache-Control", "no-store");
    try {
      RequestUser user = RequestUser.from(request);
      boolean configured = tenantService.isConfigured();

      Page page =
          pageSelector.select(
              PageSelectionCommand.builder()
                  .launcher(flag(request, "launcher"))
                  .logout(flag(request, "logout"))
                  .setup(console(request))
                  .admin(user.isAdmin())
                  .configured(configured)
                  .hasLogin(userAccountService.hasLogin(user.name()))
                  .build());
      HttpJson.writeHtml(
          response, HttpServletResponse.SC_OK, pageRenderer.render(page, request, user));
    } catch (Exception e) {
      HttpJson.writeHtml(
          response,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          pageRenderer.renderError(e.getMessage()));
    }
  }
}

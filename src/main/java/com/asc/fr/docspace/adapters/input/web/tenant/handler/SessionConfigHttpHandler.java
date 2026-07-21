package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.PageSelectorService;
import com.asc.fr.docspace.application.port.input.transfer.Page;
import com.asc.fr.docspace.application.port.input.transfer.PageSelectionCommand;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * SPA bootstrap: returns the page configuration (mode + nested config) as JSON so the client can
 * render and route without a per-mode server-rendered page.
 *
 * <p>The mode is still resolved server-side by {@link PageSelectorService} (it needs tenant/user
 * state); the admin-console context is detected from the Referer, so the client needs no plumbing.
 */
public class SessionConfigHttpHandler extends JsonHttpHandler {
  private final DocSpaceUserAccountService userAccountService;
  private final DocSpaceTenantService tenantService;
  private final PageSelectorService pageSelector;
  private final SessionConfigFactory sessionConfig;

  @Inject
  public SessionConfigHttpHandler(
      DocSpaceUserAccountService userAccountService,
      DocSpaceTenantService tenantService,
      PageSelectorService pageSelector,
      SessionConfigFactory sessionConfig) {
    super(RequestMethod.GET, PluginManifest.get().endpoints.session);
    this.userAccountService = userAccountService;
    this.tenantService = tenantService;
    this.pageSelector = pageSelector;
    this.sessionConfig = sessionConfig;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    RequestUser user = RequestUser.from(request);
    String origin = request.getHeader("Referer");

    boolean console =
        origin != null && origin.contains("/url" + PluginManifest.get().aliases.admin.from);
    Page page =
        pageSelector.select(
            PageSelectionCommand.builder()
                .launcher(false)
                .logout(false)
                .setup(console)
                .admin(user.isAdmin())
                .configured(tenantService.isConfigured())
                .hasLogin(userAccountService.hasLogin(user.name()))
                .build());
    return sessionConfig.configJson(page, request, user);
  }
}

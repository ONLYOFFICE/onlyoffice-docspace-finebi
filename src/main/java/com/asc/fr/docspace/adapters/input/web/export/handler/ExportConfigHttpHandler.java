package com.asc.fr.docspace.adapters.input.web.export.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.export.transfer.ExportConfigResponse;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/** Bootstrap data for the dashboard export script (URLs and readiness flags). */
public class ExportConfigHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantService tenant;
  private final DocSpaceUserAccountService users;

  @Inject
  public ExportConfigHttpHandler(DocSpaceTenantService tenant, DocSpaceUserAccountService users) {
    super(RequestMethod.GET, PluginManifest.get().endpoints.exportConfig);
    this.tenant = tenant;
    this.users = users;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) {
    RequestUser user = RequestUser.from(request);
    boolean configured = tenant.isConfigured();
    boolean hasLogin = configured && users.hasLogin(user.name());
    return new ExportConfigResponse(
        configured,
        hasLogin,
        tenant.docSpaceUrl(),
        ExportRoutes.excelExportApi(request),
        ExportRoutes.uploadAction(request));
  }
}

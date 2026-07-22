package com.asc.fr.docspace.adapters.input.web.debug;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.PluginHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class DebugDashboardHttpHandler extends PluginHttpHandler {
  private final DebugDashboardRenderer renderer;

  @Inject
  public DebugDashboardHttpHandler(DebugDashboardRenderer renderer) {
    super(RequestMethod.GET, PluginManifest.get().endpoints.pluginDebug, false);
    this.renderer = renderer;
  }

  private static int parsePage(HttpServletRequest request) {
    String raw = Requests.param(request, "page");
    if (raw.isEmpty()) return 1;
    try {
      return Math.max(1, Integer.parseInt(raw));
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  @Override
  @ExecuteFunctionRecord
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    response.setHeader("Cache-Control", "no-store");

    if (!PluginManifest.get().debug) {
      HttpJson.writeHtml(
          response, HttpServletResponse.SC_NOT_FOUND, renderer.renderError("Not found"));
      return;
    }

    if (!RequestUser.from(request).isAdmin()) {
      HttpJson.writeHtml(
          response,
          HttpServletResponse.SC_FORBIDDEN,
          renderer.renderError("Only FineBI administrators can view the debug dashboard."));
      return;
    }

    try {
      HttpJson.writeHtml(
          response, HttpServletResponse.SC_OK, renderer.render(request, parsePage(request)));
    } catch (Exception e) {
      HttpJson.writeHtml(
          response,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          renderer.renderError(e.getMessage()));
    }
  }
}

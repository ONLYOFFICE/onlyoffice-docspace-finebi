package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.PluginHttpHandler;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves the SPA shell. The shell is identical for every mode (launcher, setup, login, embed…) —
 * the frontend resolves its mode from the /session endpoint, so this handler only renders the page
 * template and inlines the bundle.
 */
public class DocSpacePageHttpHandler extends PluginHttpHandler {
  private final PageRenderer pageRenderer;

  @Inject
  public DocSpacePageHttpHandler(PageRenderer pageRenderer) {
    this(pageRenderer, PluginManifest.get().endpoints.docspace);
  }

  protected DocSpacePageHttpHandler(PageRenderer pageRenderer, String path) {
    super(RequestMethod.GET, path, false);
    this.pageRenderer = pageRenderer;
  }

  @Override
  @ExecuteFunctionRecord
  public final void handle(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    response.setHeader("Cache-Control", "no-store");
    try {
      HttpJson.writeHtml(response, HttpServletResponse.SC_OK, pageRenderer.render());
    } catch (Exception e) {
      HttpJson.writeHtml(
          response,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          pageRenderer.renderError(e.getMessage()));
    }
  }
}

package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.google.inject.Inject;

/**
 * Serves the admin console, the page embedded as the DocSpace card in FineBI's System Management.
 * Same SPA shell as {@link DocSpacePageHttpHandler} on a dedicated alias — the frontend detects the
 * console context from the URL (see SessionConfigHttpHandler's Referer check).
 */
public class AdminSettingsHttpHandler extends DocSpacePageHttpHandler {
  @Inject
  public AdminSettingsHttpHandler(PageRenderer pageRenderer) {
    super(pageRenderer, PluginManifest.get().endpoints.docspaceAdmin);
  }
}

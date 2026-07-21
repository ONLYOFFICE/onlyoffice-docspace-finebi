package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.Requests;
import javax.servlet.http.HttpServletRequest;

/** URL conventions of the import feature. */
public final class ImportRoutes {
  private ImportRoutes() {}

  /** The public-facing URL DocSpace POSTs webhook events to. */
  public static String webhookCallbackUrl(HttpServletRequest request) {
    return Requests.publicBaseUrl(request)
        + "/decision/url"
        + PluginManifest.get().aliases.webhook.from;
  }
}

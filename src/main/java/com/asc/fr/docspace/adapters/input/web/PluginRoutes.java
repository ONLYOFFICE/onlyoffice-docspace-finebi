package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.PluginManifest;
import javax.servlet.http.HttpServletRequest;

/** All URLs the plugin exposes; the single place that knows the FineBI routing conventions. */
public final class PluginRoutes {
  private PluginRoutes() {}

  private static String action(HttpServletRequest request, String name) {
    return request.getContextPath()
        + "/decision/plugin/private/"
        + PluginManifest.get().pluginId
        + name;
  }

  public static String pageUrl(HttpServletRequest request) {
    return request.getContextPath() + "/decision/url/docspace";
  }

  /**
   * Admin console (System Management card). A dedicated alias rather than ?setup=1 so the mode
   * cannot be lost with the query string.
   */
  public static String consoleUrl(HttpServletRequest request) {
    return request.getContextPath() + "/decision/url" + PluginManifest.get().aliases.admin.from;
  }

  /** Admin-only debug dashboard listing the stored DocSpace/FineBI file links. */
  public static String debugUrl(HttpServletRequest request) {
    return request.getContextPath() + "/decision/url" + PluginManifest.get().aliases.debug.from;
  }

  /** SSE stream shared by dataset-refresh and tenant-reset notifications. */
  public static String syncEventsUrl(HttpServletRequest request) {
    return request.getContextPath() + "/decision/url" + PluginManifest.get().aliases.events.from;
  }

  public static String setupAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.setup);
  }

  public static String loginAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.login);
  }

  public static String logoutAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.logout);
  }

  public static String resetAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.reset);
  }

  public static String importAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.importFile);
  }

  public static String foldersAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.folders);
  }

  public static String webhookRegisterAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.webhookRegister);
  }
}

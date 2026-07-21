package com.asc.fr.docspace.adapters.input.web.export.handler;

import com.asc.fr.docspace.PluginManifest;
import javax.servlet.http.HttpServletRequest;

/** URLs for the export feature. */
final class ExportRoutes {
  private ExportRoutes() {}

  private static String action(HttpServletRequest request, String name) {
    return request.getContextPath()
        + "/decision/plugin/private/"
        + PluginManifest.get().pluginId
        + name;
  }

  static String configAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.exportConfig);
  }

  static String uploadAction(HttpServletRequest request) {
    return action(request, PluginManifest.get().endpoints.exportUpload);
  }

  static String excelExportApi(HttpServletRequest request) {
    return request.getContextPath() + "/decision/v5/design/report/data/global/export/excel";
  }
}

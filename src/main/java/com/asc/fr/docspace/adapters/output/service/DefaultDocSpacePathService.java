package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.output.docspace.DocSpacePathService;

public final class DefaultDocSpacePathService implements DocSpacePathService {
  private static final String DEFAULT_EXPORT_FILENAME = "fine-export.xlsx";

  @Override
  public String sanitizeFileName(String fileName) {
    String value = fileName == null ? "" : fileName.trim();
    if (value.isEmpty()) return DEFAULT_EXPORT_FILENAME;

    value = value.replace('\\', '_').replace('/', '_');
    if (!value.toLowerCase().endsWith(".xlsx")) value = value + ".xlsx";

    return value;
  }

  @Override
  public String absolutize(String baseUrl, String url) {
    if (url == null || url.isEmpty()) return url;

    if (url.startsWith("http://") || url.startsWith("https://")) return url;

    String base = baseUrl == null ? "" : baseUrl;
    return base + (url.startsWith("/") ? "" : "/") + url;
  }
}

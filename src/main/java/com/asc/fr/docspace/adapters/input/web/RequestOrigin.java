package com.asc.fr.docspace.adapters.input.web;

import javax.servlet.http.HttpServletRequest;

/** Resolves the FineBI origin as seen by the browser, honoring reverse-proxy headers. */
public final class RequestOrigin {
  private RequestOrigin() {}

  private static String firstNonEmpty(String... values) {
    for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();

    return "";
  }

  public static String of(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.trim().isEmpty()) return origin.trim();

    String scheme = firstNonEmpty(request.getHeader("X-Forwarded-Proto"), request.getScheme());
    String host = request.getHeader("X-Forwarded-Host");
    if (host == null || host.trim().isEmpty()) {
      host = request.getServerName();
      int port = request.getServerPort();
      boolean defaultPort =
          ("http".equalsIgnoreCase(scheme) && port == 80)
              || ("https".equalsIgnoreCase(scheme) && port == 443);
      if (port > 0 && !defaultPort) host = host + ":" + port;
    }

    return scheme + "://" + host.trim();
  }
}

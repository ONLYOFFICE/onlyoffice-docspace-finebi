package com.asc.fr.docspace.adapters.input.web;

import javax.servlet.http.HttpServletRequest;

/** Servlet request helpers shared by all plugin endpoints. */
public final class Requests {
  private Requests() {}

  /** Trimmed request parameter, never null. */
  public static String param(HttpServletRequest request, String name) {
    String value = request.getParameter(name);
    return value != null ? value.trim() : "";
  }

  /** Cookie header of the request, never null. */
  public static String cookieHeader(HttpServletRequest request) {
    String cookie = request.getHeader("Cookie");
    return cookie != null ? cookie : "";
  }

  /**
   * Base URL of FineBI's decision servlet as reachable from inside the JVM. Uses the server's local
   * address, not the client-facing hostname: the plugin runs inside FineBI's JVM, so the internal
   * listener is always plain HTTP even when a reverse proxy terminates TLS externally.
   */
  public static String decisionBase(HttpServletRequest request) {
    return "http://"
        + request.getLocalName()
        + ":"
        + request.getLocalPort()
        + request.getContextPath()
        + "/decision";
  }

  /**
   * Public-facing base URL ({@code scheme://host[:port]/context}) as seen by the browser, honoring
   * reverse-proxy headers. External services (e.g. DocSpace webhooks) must use this address, not
   * the internal listener.
   */
  public static String publicBaseUrl(HttpServletRequest request) {
    String scheme = request.getHeader("X-Forwarded-Proto");
    if (scheme == null || scheme.isEmpty()) scheme = request.getScheme();

    String host = request.getHeader("X-Forwarded-Host");
    if (host == null || host.isEmpty()) {
      host = request.getServerName();
      int port = request.getServerPort();
      boolean standard =
          ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
      if (!standard) host = host + ":" + port;
    }

    return scheme + "://" + host + request.getContextPath();
  }
}

package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.application.exception.PayloadTooLargeStatusException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;

/** Servlet request helpers shared by all plugin endpoints. */
public final class Requests {
  private static final int MAX_JSON_BYTES = 256 * 1024;

  private Requests() {}

  /** Trimmed request parameter, never null. */
  public static String param(HttpServletRequest request, String name) {
    String value = request.getParameter(name);
    return value != null ? value.trim() : "";
  }

  /**
   * Entire raw request body — a webhook payload, an uploaded file. Refuses with 413 once {@code
   * maxBytes} is exceeded, so an oversized (or hostile) payload cannot exhaust the heap: the
   * declared Content-Length is rejected up front, and the limit is enforced again while streaming
   * because chunked requests carry no length and clients can lie.
   */
  public static byte[] body(HttpServletRequest request, int maxBytes) throws IOException {
    if (request.getContentLengthLong() > maxBytes)
      throw new PayloadTooLargeStatusException(maxBytes);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream in = request.getInputStream()) {
      byte[] chunk = new byte[8192];
      int read;
      while ((read = in.read(chunk)) != -1) {
        if (buffer.size() + read > maxBytes) throw new PayloadTooLargeStatusException(maxBytes);
        buffer.write(chunk, 0, read);
      }
    }

    return buffer.toByteArray();
  }

  /**
   * JSON request body bound to {@code type}; an empty body binds an empty object, missing fields
   * stay null. Inputs travel as JSON in the POST body — never in the query string, where
   * credentials and identifiers would leak into access logs, browser history, and Referers.
   */
  public static <T> T json(HttpServletRequest request, Class<T> type) throws IOException {
    return Json.read(new String(body(request, MAX_JSON_BYTES), StandardCharsets.UTF_8), type);
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

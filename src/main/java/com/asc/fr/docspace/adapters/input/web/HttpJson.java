package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.adapters.format.Json;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;

/** JSON response conventions shared by all plugin endpoints. */
public final class HttpJson {
  private HttpJson() {}

  /**
   * Writes {@code payload} as {@code application/json}: Strings are treated as pre-rendered JSON,
   * everything else is serialized with Jackson.
   */
  public static void write(HttpServletResponse response, int status, Object payload)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    PrintWriter writer = response.getWriter();
    writer.write(payload instanceof String ? (String) payload : Json.write(payload));
    writer.flush();
  }

  public static void writeHtml(HttpServletResponse response, int status, String html)
      throws IOException {
    response.setStatus(status);
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter writer = response.getWriter();
    writer.write(html);
    writer.flush();
  }

  /** Innermost cause, for error responses that must stay debuggable without server logs. */
  public static String rootCause(Throwable t) {
    Throwable root = t;
    while (root.getCause() != null && root.getCause() != root) root = root.getCause();
    return root.toString();
  }
}

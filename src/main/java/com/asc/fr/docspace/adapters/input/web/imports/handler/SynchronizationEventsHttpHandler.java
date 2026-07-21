package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.fr.decision.fun.impl.BaseHttpHandler;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Server-Sent Events stream. The browser connects once; the server holds the connection open and
 * writes "data: {}\n\n" whenever a webhook sync completes. The connection is closed after 5 minutes
 * so the servlet thread is returned to the pool — EventSource reconnects automatically.
 */
public class SynchronizationEventsHttpHandler extends BaseHttpHandler {
  private static final int KEEPALIVE_MS = 25_000;
  private static final int MAX_HOLD_MS = 5 * 60_000;

  private final SynchronizationEventBroadcaster broadcaster;

  @Inject
  public SynchronizationEventsHttpHandler(SynchronizationEventBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @Override
  public RequestMethod getMethod() {
    return RequestMethod.GET;
  }

  @Override
  public String getPath() {
    return PluginManifest.get().endpoints.syncEvents;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    response.setContentType("text/event-stream");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    response.setHeader("X-Accel-Buffering", "no");
    response.flushBuffer();

    PrintWriter writer = response.getWriter();
    broadcaster.register(writer);
    try {
      long deadline = System.currentTimeMillis() + MAX_HOLD_MS;
      while (System.currentTimeMillis() < deadline && !writer.checkError()) {
        Thread.sleep(KEEPALIVE_MS);
        writer.print(": keepalive\n\n");
        writer.flush();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      broadcaster.unregister(writer);
    }
  }
}

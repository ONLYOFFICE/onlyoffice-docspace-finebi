package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.PluginHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.SseConnection;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.PrintWriter;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Server-Sent Events stream. The browser connects once; the server holds the connection open and
 * {@link SynchronizationEventBroadcaster} writes frames (and keepalives) whenever a webhook sync
 * completes. The connection is closed after 5 minutes — EventSource reconnects automatically.
 *
 * <p>When the container supports async servlets the response is parked without a thread; otherwise
 * the handler falls back to holding its servlet thread for the duration.
 */
public class SynchronizationEventsHttpHandler extends PluginHttpHandler {
  private static final long MAX_HOLD_MS = 5 * 60_000;

  private final SynchronizationEventBroadcaster broadcaster;

  @Inject
  public SynchronizationEventsHttpHandler(SynchronizationEventBroadcaster broadcaster) {
    super(RequestMethod.GET, PluginManifest.get().endpoints.syncEvents, false);
    this.broadcaster = broadcaster;
  }

  private void holdAsync(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    AsyncContext context = request.startAsync(request, response);
    context.setTimeout(MAX_HOLD_MS);
    SseConnection connection = new SseConnection(context, response.getWriter());
    context.addListener(
        new AsyncListener() {
          @Override
          public void onComplete(AsyncEvent event) {
            broadcaster.unregister(connection);
          }

          @Override
          public void onTimeout(AsyncEvent event) {
            broadcaster.unregister(connection);
          }

          @Override
          public void onError(AsyncEvent event) {
            broadcaster.unregister(connection);
          }

          @Override
          public void onStartAsync(AsyncEvent event) {}
        });
    broadcaster.register(connection);
  }

  private void holdBlocking(HttpServletResponse response) throws Exception {
    PrintWriter writer = response.getWriter();
    SseConnection connection = new SseConnection(writer);
    broadcaster.register(connection);
    try {
      long deadline = System.currentTimeMillis() + MAX_HOLD_MS;
      while (System.currentTimeMillis() < deadline && !writer.checkError()) Thread.sleep(1_000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      broadcaster.unregister(connection);
    }
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    response.setContentType("text/event-stream");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    response.setHeader("X-Accel-Buffering", "no");
    response.flushBuffer();

    if (request.isAsyncSupported()) holdAsync(request, response);
    else holdBlocking(response);
  }
}

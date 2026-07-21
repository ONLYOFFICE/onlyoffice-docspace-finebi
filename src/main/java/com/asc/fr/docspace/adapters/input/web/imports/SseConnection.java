package com.asc.fr.docspace.adapters.input.web.imports;

import java.io.PrintWriter;
import javax.servlet.AsyncContext;

/**
 * One open SSE subscriber. Wraps the response writer plus, when the container supports async
 * servlets, the {@link AsyncContext} that keeps the response open without a parked thread.
 */
public final class SseConnection {
  private final AsyncContext context;
  private final PrintWriter writer;

  /** Async connection: closing completes the context and releases the response. */
  public SseConnection(AsyncContext context, PrintWriter writer) {
    this.context = context;
    this.writer = writer;
  }

  /** Blocking connection: the handler thread owns the response lifecycle. */
  public SseConnection(PrintWriter writer) {
    this(null, writer);
  }

  /**
   * @return false once the client has disconnected and the connection should be dropped.
   */
  public boolean send(String frame) {
    synchronized (writer) {
      writer.print(frame);
      writer.flush();
    }
    return !writer.checkError();
  }

  public void close() {
    if (context == null) return;

    try {
      context.complete();
    } catch (RuntimeException ignored) {
    }
  }
}

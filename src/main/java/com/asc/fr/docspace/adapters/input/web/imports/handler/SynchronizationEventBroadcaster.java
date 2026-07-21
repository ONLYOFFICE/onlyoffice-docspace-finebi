package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import java.io.PrintWriter;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE-backed {@link SynchronizationEventPublisher}: holds open connections and writes a "data
 * refreshed" frame to all of them when a webhook sync completes.
 */
public final class SynchronizationEventBroadcaster implements SynchronizationEventPublisher {
  private final CopyOnWriteArrayList<PrintWriter> clients = new CopyOnWriteArrayList<>();

  private void broadcast(String frame) {
    for (PrintWriter writer : clients) {
      try {
        writer.print(frame);
        writer.flush();
      } catch (Exception ignored) {
      }
    }
  }

  void register(PrintWriter writer) {
    clients.add(writer);
  }

  void unregister(PrintWriter writer) {
    clients.remove(writer);
  }

  // TODO: Use some helpers/parsers
  @Override
  public void datasetUpdated(String tableName) {
    String escaped = tableName.replace("\\", "\\\\").replace("\"", "\\\"");
    broadcast(
        "data: {\"type\":\""
            + PluginManifest.get().events.backend.datasetUpdated
            + "\",\"tableName\":\""
            + escaped
            + "\"}\n\n");
  }

  @Override
  public void tenantReset() {
    broadcast("data: {\"type\":\"" + PluginManifest.get().events.backend.tenantReset + "\"}\n\n");
  }
}

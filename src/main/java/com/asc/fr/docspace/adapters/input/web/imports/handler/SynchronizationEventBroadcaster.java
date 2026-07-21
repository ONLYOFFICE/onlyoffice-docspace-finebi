package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.input.web.imports.SseConnection;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE-backed {@link SynchronizationEventPublisher}: holds open connections and writes a "data
 * refreshed" frame to all of them when a webhook sync completes. A single shared timer pings every
 * connection each {@link #KEEPALIVE_MS} so proxies keep the streams open and dead clients are
 * detected and dropped — no per-connection thread required.
 */
public final class SynchronizationEventBroadcaster implements SynchronizationEventPublisher {
  private static final long KEEPALIVE_MS = 25_000;
  private static final String KEEPALIVE_FRAME = ": keepalive\n\n";

  private final CopyOnWriteArrayList<SseConnection> clients = new CopyOnWriteArrayList<>();

  @Inject
  SynchronizationEventBroadcaster(TaskSchedulerService scheduler) {
    keepalive(scheduler);
  }

  private void keepalive(TaskSchedulerService scheduler) {
    scheduler.schedule(
        KEEPALIVE_MS,
        () -> {
          keepalive(scheduler);
          broadcast(KEEPALIVE_FRAME);
        });
  }

  void register(SseConnection connection) {
    clients.add(connection);
  }

  void unregister(SseConnection connection) {
    clients.remove(connection);
  }

  private void broadcast(String frame) {
    for (SseConnection connection : clients) {
      boolean alive;
      try {
        alive = connection.send(frame);
      } catch (Exception e) {
        alive = false;
      }
      if (!alive) {
        clients.remove(connection);
        connection.close();
      }
    }
  }

  private void broadcastEvent(ObjectNode payload) {
    broadcast("data: " + payload + "\n\n");
  }

  @Override
  public void datasetUpdated(String tableName) {
    broadcastEvent(
        Json.MAPPER
            .createObjectNode()
            .put("type", PluginManifest.get().events.backend.datasetUpdated)
            .put("tableName", tableName));
  }

  @Override
  public void tenantReset() {
    broadcastEvent(
        Json.MAPPER
            .createObjectNode()
            .put("type", PluginManifest.get().events.backend.tenantReset));
  }
}

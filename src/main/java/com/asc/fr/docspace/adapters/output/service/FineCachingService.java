package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.output.CachingService;
import com.fr.store.StateHubManager;
import com.fr.store.StateHubService;
import java.util.concurrent.TimeUnit;

/**
 * Cluster-wide {@link CachingService} backed by FineBI's StateHub: entries live in the platform
 * state store (in-process when standalone, the shared state server in a cluster), so a value put on
 * one node is visible to — and invalidated for — every node at once.
 *
 * <p>Values must be serializable and must not contain secrets, because entries leave process
 * memory. Every StateHub failure is swallowed and treated as a cache miss, so a state-server hiccup
 * degrades to uncached calls instead of failing the caller. {@code maximumEntries} cannot be
 * enforced remotely; the TTL bounds growth instead.
 */
public final class FineCachingService implements CachingService {
  private static final String SERVICE_PREFIX = "plugin-docspace-";

  @Override
  public <K, V> Cache<K, V> create(String name, long ttlSeconds, long maximumEntries) {
    int ttlMillis = (int) Math.min(TimeUnit.SECONDS.toMillis(ttlSeconds), Integer.MAX_VALUE);
    String serviceName = SERVICE_PREFIX + name;

    return new Cache<K, V>() {
      private StateHubService hub() {
        return StateHubManager.applyForService(serviceName);
      }

      @Override
      public V get(K key) {
        try {
          return hub().get(String.valueOf(key));
        } catch (Exception ignored) {
          return null;
        }
      }

      @Override
      public void put(K key, V value) {
        try {
          hub().put(String.valueOf(key), value, ttlMillis);
        } catch (Exception ignored) {
        }
      }

      @Override
      public void invalidate(K key) {
        try {
          hub().delete(String.valueOf(key));
        } catch (Exception ignored) {
        }
      }

      @Override
      public void invalidateAll() {
        try {
          hub().clearAll();
        } catch (Exception ignored) {
        }
      }
    };
  }
}

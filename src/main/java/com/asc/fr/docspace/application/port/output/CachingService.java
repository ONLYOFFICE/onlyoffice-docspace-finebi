package com.asc.fr.docspace.application.port.output;

/**
 * Output port for short-lived cluster-wide caches: an entry put on one node is visible to — and
 * invalidated for — every node. Values leave process memory, so never cache secrets through this
 * port; per-node caching of decrypted values belongs inside the owning adapter instead.
 */
public interface CachingService {
  interface Cache<K, V> {
    V get(K key);

    void put(K key, V value);

    void invalidate(K key);

    void invalidateAll();
  }

  /**
   * Creates a cache whose entries expire {@code ttlSeconds} after being written. {@code name}
   * isolates the cache from others created by the same service;
   */
  <K, V> Cache<K, V> create(String name, long ttlSeconds, long maximumEntries);
}

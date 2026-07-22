package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.output.ClusterLockService;
import com.fr.cluster.ClusterBridge;
import com.fr.cluster.lock.ClusterLock;
import com.fr.cluster.lock.ClusterLockFactory;

/**
 * {@link ClusterLockService} backed by FineBI's cluster lock ({@link
 * ClusterBridge#getLockFactory}). In a cluster this is the Redis-backed lock, which carries its own
 * lease so a node that dies while holding it does not block the others forever; in standalone mode
 * it is a local lock that is always granted. {@code tryLock()} is non-blocking, so a node that
 * loses the race simply skips the tick rather than queueing up to run the same work moments later.
 *
 * <p>Fails open: if the lock subsystem cannot be reached the task is run unguarded, because the
 * clustered jobs are idempotent and running twice is safer than not running at all.
 */
public final class FineClusterLockService implements ClusterLockService {
  private static final String LOCK_PREFIX = "plugin-docspace-";

  @Override
  public void runExclusive(String lockName, Runnable task) {
    ClusterLock lock = lock(lockName);
    if (lock == null) {
      task.run();
      return;
    }

    boolean acquired;
    try {
      acquired = lock.tryLock();
    } catch (Exception unavailable) {
      task.run();
      return;
    }

    if (!acquired) return;

    try {
      task.run();
    } finally {
      try {
        lock.unlock();
      } catch (Exception ignored) {
        // Best-effort release; a cluster lease reclaims it if this fails.
      }
    }
  }

  private static ClusterLock lock(String lockName) {
    try {
      ClusterLockFactory factory = ClusterBridge.getLockFactory();
      return factory == null ? null : factory.get(LOCK_PREFIX + lockName);
    } catch (Exception unavailable) {
      return null;
    }
  }
}

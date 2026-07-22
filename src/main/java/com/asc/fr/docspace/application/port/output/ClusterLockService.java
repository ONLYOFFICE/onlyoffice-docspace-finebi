package com.asc.fr.docspace.application.port.output;

/**
 * Cluster-wide mutual exclusion for background work. Lets a job run on exactly one node at a time
 * across a cluster.
 */
public interface ClusterLockService {
  /**
   * Runs {@code task} only if this node acquires {@code lockName} right now, then always releases
   * it. If another node holds the lock the task is skipped (non-blocking). If the lock subsystem is
   * unavailable the task is run anyway, so callers must be idempotent.
   */
  void runExclusive(String lockName, Runnable task);
}

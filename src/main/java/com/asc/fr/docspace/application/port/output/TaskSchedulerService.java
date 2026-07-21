package com.asc.fr.docspace.application.port.output;

/**
 * Output port for fire-and-forget background work (webhook registration, webhook-triggered
 * re-syncs). Lets services stay free of threading concerns and lets tests run tasks synchronously.
 */
public interface TaskSchedulerService {
  interface Cancellable {
    void cancel();
  }

  /** Runs {@code task} off the caller's thread as soon as possible. */
  void run(Runnable task);

  /**
   * Runs {@code task} after at least {@code delay} milliseconds.
   *
   * @return a handle that cancels the task if it has not started yet; cancelling a task that
   *     already ran (or is running) is a no-op
   */
  Cancellable schedule(long delay, Runnable task);
}

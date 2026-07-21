package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

/**
 * {@link TaskSchedulerService} backed by a small named daemon pool, so background work is bounded,
 * identifiable in thread dumps, and never prevents JVM shutdown.
 */
public final class FineTaskScheduler implements TaskSchedulerService {
  private final ScheduledExecutorService pool;

  public FineTaskScheduler() {
    this.pool =
        Executors.newScheduledThreadPool(
            2,
            new ThreadFactory() {
              private final AtomicInteger counter = new AtomicInteger();

              @Override
              public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r, "worker-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
              }
            });
  }

  private static Runnable guarded(Runnable task) {
    return () -> {
      try {
        task.run();
      } catch (Exception e) {
        // TODO: Handle it somehow
      }
    };
  }

  @Override
  public void run(Runnable task) {
    pool.execute(guarded(task));
  }

  @Override
  public Cancellable schedule(long delayMs, Runnable task) {
    ScheduledFuture<?> future = pool.schedule(guarded(task), delayMs, TimeUnit.MILLISECONDS);
    return () -> future.cancel(false);
  }
}

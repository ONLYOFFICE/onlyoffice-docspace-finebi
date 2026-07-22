package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

/**
 * {@link TaskSchedulerService} backed by two small named daemon pools.
 *
 * <p>A {@link ScheduledThreadPoolExecutor} keeps time only: its {@code DelayedWorkQueue} is
 * unbounded and cannot be replaced, so it never runs work directly — it just fires a trigger that
 * hands the task to the worker pool. The worker is a plain {@link ThreadPoolExecutor} with a
 * <b>bounded</b> queue and a {@link ThreadPoolExecutor.CallerRunsPolicy}: once the queue fills, the
 * submitting thread runs the task itself, throttling producers instead of letting the backlog grow
 * without bound (or dropping work). Both pools use daemon threads so background work never blocks
 * JVM shutdown.
 */
public final class FineTaskSchedulerService implements TaskSchedulerService {
  private static final int WORKER_QUEUE_CAPACITY = 128;
  private static final int SCHEDULER_THREADS = 1;
  private static final int WORKER_THREADS = 2;

  private final ScheduledThreadPoolExecutor scheduler;
  private final ThreadPoolExecutor worker;

  public FineTaskSchedulerService() {
    this.scheduler =
        new ScheduledThreadPoolExecutor(SCHEDULER_THREADS, namedDaemonFactory("fine-scheduler"));
    this.scheduler.setRemoveOnCancelPolicy(true);

    this.worker =
        new ThreadPoolExecutor(
            WORKER_THREADS,
            WORKER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(WORKER_QUEUE_CAPACITY),
            namedDaemonFactory("fine-worker"),
            new ThreadPoolExecutor.CallerRunsPolicy());
  }

  private static ThreadFactory namedDaemonFactory(String prefix) {
    return new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      }
    };
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

  /**
   * Hands {@code task} to the worker pool from a scheduler thread. Swallows the rejection that only
   * happens once the pools are shutting down, so it never escapes to cancel a periodic schedule.
   */
  private void dispatch(Runnable task) {
    try {
      worker.execute(guarded(task));
    } catch (RejectedExecutionException shuttingDown) {
    }
  }

  @Override
  public void run(Runnable task) {
    dispatch(task);
  }

  @Override
  public Cancellable schedule(long delayMs, Runnable task) {
    ScheduledFuture<?> future =
        scheduler.schedule(() -> dispatch(task), delayMs, TimeUnit.MILLISECONDS);
    return () -> future.cancel(false);
  }

  @Override
  public Cancellable scheduleAtFixedRate(long initialDelayMs, long periodMs, Runnable task) {
    ScheduledFuture<?> future =
        scheduler.scheduleAtFixedRate(
            () -> dispatch(task), initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    return () -> future.cancel(false);
  }
}

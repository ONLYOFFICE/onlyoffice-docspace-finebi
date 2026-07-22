package com.asc.fr.docspace.adapters.output.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.input.ScheduledJob;
import com.asc.fr.docspace.application.port.output.ClusterLockService;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FineScheduledJobRunnerTest {

  /** Fires every scheduled task once, synchronously, so the wiring can be observed. */
  private static final class ImmediateScheduler implements TaskSchedulerService {
    @Override
    public void run(Runnable task) {
      task.run();
    }

    @Override
    public Cancellable schedule(long delayMs, Runnable task) {
      task.run();
      return () -> {};
    }

    @Override
    public Cancellable scheduleAtFixedRate(long initialDelayMs, long periodMs, Runnable task) {
      task.run();
      return () -> {};
    }
  }

  private static final class FakeClusterLock implements ClusterLockService {
    final List<String> lockNames = new ArrayList<>();
    boolean acquired = true;

    @Override
    public void runExclusive(String lockName, Runnable task) {
      lockNames.add(lockName);
      if (acquired) task.run();
    }
  }

  private static final class CountingJob implements ScheduledJob {
    int runs;

    @Override
    public String name() {
      return "plain";
    }

    @Override
    public long initialDelayMillis() {
      return 0;
    }

    @Override
    public long periodMillis() {
      return 1000;
    }

    @Override
    public void run() {
      runs++;
    }
  }

  private static final class CountingClusterJob implements ScheduledClusterJob {
    int runs;

    @Override
    public String name() {
      return "clustered";
    }

    @Override
    public long initialDelayMillis() {
      return 0;
    }

    @Override
    public long periodMillis() {
      return 1000;
    }

    @Override
    public void run() {
      runs++;
    }
  }

  @Test
  void givenClusterJob_whenLockAcquired_thenRunsThroughLockKeyedOnName() {
    CountingClusterJob job = new CountingClusterJob();
    FakeClusterLock lock = new FakeClusterLock();

    new FineScheduledJobRunner(
        Collections.emptySet(), Collections.singleton(job), new ImmediateScheduler(), lock);

    assertEquals(1, job.runs);
    assertEquals(Collections.singletonList("clustered"), lock.lockNames);
  }

  @Test
  void givenClusterJob_whenLockNotAcquired_thenSkipsThisNode() {
    CountingClusterJob job = new CountingClusterJob();
    FakeClusterLock lock = new FakeClusterLock();
    lock.acquired = false;

    new FineScheduledJobRunner(
        Collections.emptySet(), Collections.singleton(job), new ImmediateScheduler(), lock);

    assertEquals(0, job.runs);
    assertEquals(Collections.singletonList("clustered"), lock.lockNames);
  }

  @Test
  void givenPlainJob_whenScheduling_thenRunsWithoutTheClusterLock() {
    CountingJob job = new CountingJob();
    FakeClusterLock lock = new FakeClusterLock();

    new FineScheduledJobRunner(
        Collections.<ScheduledJob>singleton(job),
        Collections.<ScheduledClusterJob>emptySet(),
        new ImmediateScheduler(),
        lock);

    assertEquals(1, job.runs);
    assertTrue(lock.lockNames.isEmpty());
  }
}

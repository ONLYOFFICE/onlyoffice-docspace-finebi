package com.asc.fr.docspace.adapters.output.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.input.ScheduledJob;
import com.asc.fr.docspace.application.port.output.ClusterLockService;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FineScheduledJobRunnerTest {
  @Mock private TaskSchedulerService scheduler;
  @Mock private ClusterLockService clusterLock;

  @BeforeEach
  void runScheduledTasksImmediately() {
    when(scheduler.scheduleAtFixedRate(anyLong(), anyLong(), any()))
        .thenAnswer(
            invocation -> {
              invocation.getArgument(2, Runnable.class).run();
              return (TaskSchedulerService.Cancellable) () -> {};
            });
  }

  private FineScheduledJobRunner runnerFor(ScheduledJob plainJob) {
    return new FineScheduledJobRunner(
        Collections.singleton(plainJob), Collections.emptySet(), scheduler, clusterLock);
  }

  private FineScheduledJobRunner runnerFor(ScheduledClusterJob clusterJob) {
    return new FineScheduledJobRunner(
        Collections.emptySet(), Collections.singleton(clusterJob), scheduler, clusterLock);
  }

  @Test
  void givenClusterJob_whenLockAcquired_thenRunsThroughLockKeyedOnName() {
    ScheduledClusterJob job = clusterJob("clustered");
    doAnswer(invocation -> runTask(invocation.getArgument(1)))
        .when(clusterLock)
        .runExclusive(eq("clustered"), any());

    runnerFor(job);

    verify(clusterLock).runExclusive(eq("clustered"), any());
    verify(job).run();
  }

  @Test
  void givenClusterJob_whenLockNotAcquired_thenSkipsThisNode() {
    ScheduledClusterJob job = clusterJob("clustered");

    runnerFor(job);

    verify(clusterLock).runExclusive(eq("clustered"), any());
    verify(job, never()).run();
  }

  @Test
  void givenPlainJob_whenScheduling_thenRunsWithoutTheClusterLock() {
    ScheduledJob job = mock(ScheduledJob.class);

    runnerFor(job);

    verify(job).run();
    verifyNoInteractions(clusterLock);
  }

  private static ScheduledClusterJob clusterJob(String name) {
    ScheduledClusterJob job = mock(ScheduledClusterJob.class);
    when(job.name()).thenReturn(name);
    return job;
  }

  private static Object runTask(Runnable task) {
    task.run();
    return null;
  }
}

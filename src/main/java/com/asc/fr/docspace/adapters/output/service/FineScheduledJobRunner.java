package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.input.ScheduledJob;
import com.asc.fr.docspace.application.port.output.ClusterLockService;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.google.inject.Inject;
import java.util.Set;

/**
 * Generic bridge between the scheduled jobs and the {@link TaskSchedulerService}. On creation it
 * registers every job at its declared fixed rate; it knows nothing about what any individual job
 * does, so new recurring work is added simply by binding another job.
 *
 * <ul>
 *   <li>{@link ScheduledJob}s run on every node.
 *   <li>{@link ScheduledClusterJob}s run through {@link ClusterLockService}, so exactly one node in
 *       a cluster executes each tick.
 * </ul>
 */
public final class FineScheduledJobRunner {
  @Inject
  public FineScheduledJobRunner(
      Set<ScheduledJob> jobs,
      Set<ScheduledClusterJob> clusterJobs,
      TaskSchedulerService scheduler,
      ClusterLockService clusterLock) {
    for (ScheduledJob job : jobs)
      scheduler.scheduleAtFixedRate(job.initialDelayMillis(), job.periodMillis(), job::run);

    for (ScheduledClusterJob job : clusterJobs)
      scheduler.scheduleAtFixedRate(
          job.initialDelayMillis(),
          job.periodMillis(),
          () -> clusterLock.runExclusive(job.name(), job::run));
  }
}

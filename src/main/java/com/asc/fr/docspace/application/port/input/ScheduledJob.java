package com.asc.fr.docspace.application.port.input;

/**
 * A unit of recurring background work. Implementations hold the business logic; the schedule
 * metadata tells the infrastructure how often to run it. The runner drives every registered job
 * through {@link com.asc.fr.docspace.application.port.output.TaskSchedulerService}, so jobs stay
 * free of any threading or platform concerns and are directly unit-testable by calling {@link
 * #run()}.
 */
public interface ScheduledJob {
  /** Stable identifier, used for diagnostics and to namespace the job's schedule. */
  String name();

  /** Milliseconds to wait after startup before the first run. */
  long initialDelayMillis();

  /** Milliseconds between the start of one run and the next. */
  long periodMillis();

  /** Performs one pass. Must be idempotent; a thrown exception is swallowed by the runner. */
  void run();
}

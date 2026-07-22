package com.asc.fr.docspace.application.job;

public final class JobSchedule {
  public final long initialDelayMillis;
  public final long periodMillis;

  public JobSchedule(long initialDelayMillis, long periodMillis) {
    this.initialDelayMillis = initialDelayMillis;
    this.periodMillis = periodMillis;
  }
}

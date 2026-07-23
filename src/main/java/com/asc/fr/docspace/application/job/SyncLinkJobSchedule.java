package com.asc.fr.docspace.application.job;

public final class SyncLinkJobSchedule {
  public final long initialDelayMillis;
  public final long periodMillis;
  public final long staleAfterMillis;
  public final int maxPerRun;

  public SyncLinkJobSchedule(
      long initialDelayMillis, long periodMillis, long staleAfterMillis, int maxPerRun) {
    this.initialDelayMillis = initialDelayMillis;
    this.periodMillis = periodMillis;
    this.staleAfterMillis = staleAfterMillis;
    this.maxPerRun = maxPerRun;
  }
}

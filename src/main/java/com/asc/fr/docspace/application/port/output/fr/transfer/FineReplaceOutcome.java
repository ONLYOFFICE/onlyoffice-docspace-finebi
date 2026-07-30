package com.asc.fr.docspace.application.port.output.fr.transfer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class FineReplaceOutcome {
  public enum Status {
    REPLACED,
    ABSENT,
    FAILED
  }

  private final String tableId;
  private final Status status;
  private final String name;
  private final String detail;

  public static FineReplaceOutcome replaced(String tableId, String name) {
    return new FineReplaceOutcome(tableId, Status.REPLACED, name == null ? "" : name, "");
  }

  public static FineReplaceOutcome absent(String tableId) {
    return new FineReplaceOutcome(tableId, Status.ABSENT, "", "");
  }

  public static FineReplaceOutcome failed(String tableId, String detail) {
    return new FineReplaceOutcome(tableId, Status.FAILED, "", detail == null ? "" : detail);
  }
}

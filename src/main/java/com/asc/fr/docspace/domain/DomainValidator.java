package com.asc.fr.docspace.domain;

import java.util.function.Supplier;

public final class DomainValidator {
  private DomainValidator() {}

  public static String requirePresent(
      String value, Supplier<? extends RuntimeException> exception) {
    if (value == null || value.trim().isEmpty()) throw exception.get();
    return value.trim();
  }
}

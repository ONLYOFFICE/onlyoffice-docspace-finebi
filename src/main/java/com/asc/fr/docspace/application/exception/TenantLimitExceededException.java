package com.asc.fr.docspace.application.exception;

import java.io.IOException;

/**
 * Thrown when changing tenants would add a distinct saved-tenant history entry beyond the fixed
 * cap.
 */
public class TenantLimitExceededException extends IOException {
  private final String code;

  public TenantLimitExceededException(String message) {
    this(message, null);
  }

  /**
   * @param code stable frontend i18n key for {@code message}, or {@code null} when there isn't one.
   */
  public TenantLimitExceededException(String message, String code) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}

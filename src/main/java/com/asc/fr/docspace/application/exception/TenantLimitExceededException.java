package com.asc.fr.docspace.application.exception;

import java.io.IOException;

/**
 * Thrown when changing tenants would add a distinct saved-tenant history entry beyond the fixed cap.
 */
public class TenantLimitExceededException extends IOException {
  public TenantLimitExceededException(String message) {
    super(message);
  }
}

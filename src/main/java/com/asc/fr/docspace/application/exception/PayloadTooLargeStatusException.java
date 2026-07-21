package com.asc.fr.docspace.application.exception;

public final class PayloadTooLargeStatusException extends PluginStatusException {
  public PayloadTooLargeStatusException(int maxBytes) {
    super(413, "Request body is too large (limit " + maxBytes + " bytes)");
  }
}

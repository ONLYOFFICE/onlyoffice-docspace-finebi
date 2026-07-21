package com.asc.fr.docspace.application.exception;

public final class BadRequestStatusException extends PluginStatusException {
  public BadRequestStatusException(String message) {
    super(400, message);
  }
}

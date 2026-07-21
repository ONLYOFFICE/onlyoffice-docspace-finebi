package com.asc.fr.docspace.application.exception;

public final class ForbiddenStatusException extends PluginStatusException {
  public ForbiddenStatusException(String message) {
    super(403, message);
  }
}

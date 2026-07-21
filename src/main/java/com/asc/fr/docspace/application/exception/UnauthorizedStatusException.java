package com.asc.fr.docspace.application.exception;

public final class UnauthorizedStatusException extends PluginStatusException {
  public UnauthorizedStatusException() {
    super(401, "Sign in to DocSpace first");
  }
}

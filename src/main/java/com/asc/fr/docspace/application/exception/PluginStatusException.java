package com.asc.fr.docspace.application.exception;

public class PluginStatusException extends RuntimeException {
  private final int status;

  public PluginStatusException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }
}

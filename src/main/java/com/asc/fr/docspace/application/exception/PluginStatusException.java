package com.asc.fr.docspace.application.exception;

import java.util.Collections;
import java.util.Map;

public class PluginStatusException extends RuntimeException {
  private final int status;
  private final String code;
  private final transient Map<String, Object> params;

  public PluginStatusException(int status, String message) {
    this(status, message, null, null);
  }

  public PluginStatusException(int status, String message, String code) {
    this(status, message, code, null);
  }

  public PluginStatusException(
      int status, String message, String code, Map<String, Object> params) {
    super(message);
    this.status = status;
    this.code = code;
    this.params = params == null ? Collections.emptyMap() : params;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public Map<String, Object> params() {
    return params;
  }
}

package com.asc.fr.docspace.application.exception;

import java.util.Map;

public final class BadRequestStatusException extends PluginStatusException {
  public BadRequestStatusException(String message) {
    super(400, message);
  }

  public BadRequestStatusException(String message, String code) {
    super(400, message, code);
  }

  public BadRequestStatusException(String message, String code, Map<String, Object> params) {
    super(400, message, code, params);
  }
}

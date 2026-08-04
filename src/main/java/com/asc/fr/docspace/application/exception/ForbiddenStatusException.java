package com.asc.fr.docspace.application.exception;

import java.util.Map;

public final class ForbiddenStatusException extends PluginStatusException {
  public ForbiddenStatusException(String message) {
    super(403, message);
  }

  public ForbiddenStatusException(String message, String code) {
    super(403, message, code);
  }

  public ForbiddenStatusException(String message, String code, Map<String, Object> params) {
    super(403, message, code, params);
  }
}

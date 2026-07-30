package com.asc.fr.docspace.application.exception;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;

/**
 * An import the user could act on (wrong file type, too many sheets, too big, nothing importable).
 * Carries a stable {@code code} (a frontend i18n key) plus interpolation {@code params} so the
 * toast can show a localized message; the English {@link #getMessage() message} is the fallback.
 */
@Getter
public final class ImportRejectedException extends IOException {
  private final String code;
  private final transient Map<String, Object> params;

  public ImportRejectedException(String code, String message) {
    this(code, message, Collections.emptyMap());
  }

  public ImportRejectedException(String code, String message, Map<String, Object> params) {
    super(message);
    this.code = code;
    this.params = params == null ? Collections.emptyMap() : params;
  }
}

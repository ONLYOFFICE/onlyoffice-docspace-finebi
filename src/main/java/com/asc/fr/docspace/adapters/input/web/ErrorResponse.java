package com.asc.fr.docspace.adapters.input.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;

/** Failure payload: {@code {"ok":false,"error":"…"}}, optionally with an i18n {@code errorCode}. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ErrorResponse extends OkResponse {
  private final String error;
  private final String errorCode;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private final Map<String, Object> errorParams;

  public ErrorResponse(String error) {
    this(error, null, Collections.emptyMap());
  }

  public ErrorResponse(String error, String errorCode, Map<String, Object> errorParams) {
    super(false);
    this.error = error;
    this.errorCode = errorCode;
    this.errorParams = errorParams == null ? Collections.emptyMap() : errorParams;
  }
}

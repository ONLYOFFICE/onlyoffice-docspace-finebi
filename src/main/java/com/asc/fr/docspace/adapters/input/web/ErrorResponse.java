package com.asc.fr.docspace.adapters.input.web;

import lombok.Getter;

/** Failure payload: {@code {"ok":false,"error":"…"}}. */
@Getter
public final class ErrorResponse extends OkResponse {
  private final String error;

  public ErrorResponse(String error) {
    super(false);
    this.error = error;
  }
}

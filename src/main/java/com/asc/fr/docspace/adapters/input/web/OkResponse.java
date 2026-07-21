package com.asc.fr.docspace.adapters.input.web;

import lombok.Getter;

/**
 * Base payload for JSON endpoints: {@code {"ok":true}}. Endpoint-specific responses subclass it to
 * add fields; serialization is plain Jackson.
 */
@Getter
public class OkResponse {
  private static final OkResponse OK = new OkResponse(true);

  private final boolean ok;

  protected OkResponse(boolean ok) {
    this.ok = ok;
  }

  public static OkResponse ok() {
    return OK;
  }
}

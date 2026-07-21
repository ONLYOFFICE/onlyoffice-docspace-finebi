package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import com.asc.fr.docspace.adapters.input.web.OkResponse;
import lombok.Getter;

@Getter
public final class WebhookRegisteredResponse extends OkResponse {
  private final String callbackUrl;

  public WebhookRegisteredResponse(String callbackUrl) {
    super(true);
    this.callbackUrl = callbackUrl;
  }
}

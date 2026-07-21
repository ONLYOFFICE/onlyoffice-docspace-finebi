package com.asc.fr.docspace.adapters.output.client.docspace.transfer.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public final class DocSpaceWebhookCreateRequest {
  @JsonProperty("name")
  private String name = "FineBI sync";

  @JsonProperty("uri")
  private String uri;

  @JsonProperty("secretKey")
  private String secretKey;

  @JsonProperty("enabled")
  private boolean enabled = true;

  @JsonProperty("ssl")
  private boolean ssl;

  public static DocSpaceWebhookCreateRequest of(String callbackUrl, String secret) {
    DocSpaceWebhookCreateRequest request = new DocSpaceWebhookCreateRequest();
    request.uri = callbackUrl;
    request.secretKey = secret;
    return request;
  }
}

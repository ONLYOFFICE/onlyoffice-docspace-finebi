package com.asc.fr.docspace.adapters.output.client.docspace.transfer.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public final class DocSpaceWebhookUpdateRequest {
  @JsonProperty("id")
  private int id;

  @JsonProperty("name")
  private String name = "FineBI Sync";

  @JsonProperty("uri")
  private String uri;

  @JsonProperty("secretKey")
  private String secretKey;

  @JsonProperty("enabled")
  private boolean enabled = true;

  @JsonProperty("ssl")
  private boolean ssl;

  public static DocSpaceWebhookUpdateRequest of(int id, String callbackUrl, String secret) {
    DocSpaceWebhookUpdateRequest request = new DocSpaceWebhookUpdateRequest();
    request.id = id;
    request.uri = callbackUrl;
    request.secretKey = secret;
    request.ssl = callbackUrl != null && callbackUrl.startsWith("https://");
    return request;
  }
}

package com.asc.fr.docspace.adapters.output.client.fr.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineTableAddItemResponse {
  @JsonProperty("name")
  private String name;

  @JsonProperty("transferName")
  private String transferName;

  @JsonProperty("success")
  private Boolean success;
}

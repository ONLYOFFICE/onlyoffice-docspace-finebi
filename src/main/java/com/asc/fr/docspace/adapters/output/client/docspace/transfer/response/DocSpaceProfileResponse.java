package com.asc.fr.docspace.adapters.output.client.docspace.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DocSpaceProfileResponse {
  @JsonProperty("isAdmin")
  private boolean admin;

  @JsonProperty("isOwner")
  private boolean owner;
}

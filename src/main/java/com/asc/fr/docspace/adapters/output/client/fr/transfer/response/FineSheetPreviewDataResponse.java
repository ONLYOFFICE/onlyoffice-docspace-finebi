package com.asc.fr.docspace.adapters.output.client.fr.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineSheetPreviewDataResponse {
  @JsonProperty("baseAttach")
  private JsonNode baseAttach;

  @JsonProperty("fields")
  private JsonNode fields;
}

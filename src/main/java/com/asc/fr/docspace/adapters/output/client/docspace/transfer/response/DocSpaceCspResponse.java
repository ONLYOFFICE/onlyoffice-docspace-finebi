package com.asc.fr.docspace.adapters.output.client.docspace.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DocSpaceCspResponse {
  @JsonProperty("domains")
  private List<String> domains = Collections.emptyList();
}

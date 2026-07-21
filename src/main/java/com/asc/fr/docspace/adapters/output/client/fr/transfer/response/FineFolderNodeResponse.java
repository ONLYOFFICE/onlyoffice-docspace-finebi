package com.asc.fr.docspace.adapters.output.client.fr.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineFolderNodeResponse {
  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("children")
  private List<FineFolderNodeResponse> children = Collections.emptyList();

  @JsonProperty("folders")
  private List<FineFolderNodeResponse> folders = Collections.emptyList();
}

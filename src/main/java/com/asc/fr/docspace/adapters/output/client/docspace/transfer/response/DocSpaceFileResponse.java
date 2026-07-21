package com.asc.fr.docspace.adapters.output.client.docspace.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DocSpaceFileResponse {
  @JsonProperty("id")
  private Long id;

  @JsonProperty("title")
  private String title;

  @JsonProperty("viewUrl")
  private String viewUrl;

  @JsonProperty("file")
  private DocSpaceFileResponse file;
}

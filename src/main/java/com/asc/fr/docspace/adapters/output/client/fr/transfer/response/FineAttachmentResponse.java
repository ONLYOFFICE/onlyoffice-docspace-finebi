package com.asc.fr.docspace.adapters.output.client.fr.transfer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineAttachmentResponse {
  @JsonProperty("path")
  private String path;

  @JsonProperty("filename")
  private String filename;

  @JsonProperty("attach_id")
  private String attachId;

  @JsonProperty("attach_type")
  private String attachType = "other";

  @JsonProperty("sheetIndex")
  private List<Integer> sheetIndex = Collections.emptyList();

  public static FineAttachmentResponse emptyPlaceholder() {
    FineAttachmentResponse response = new FineAttachmentResponse();
    response.path = "";
    response.filename = "";
    response.attachId = "";
    response.sheetIndex = Collections.emptyList();
    return response;
  }
}

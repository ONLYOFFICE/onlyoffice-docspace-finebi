package com.asc.fr.docspace.adapters.output.client.fr.transfer.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class FineCreateFolderRequest {
  @JsonProperty("name")
  private String name;

  @JsonProperty("parentId")
  private String parentId;
}

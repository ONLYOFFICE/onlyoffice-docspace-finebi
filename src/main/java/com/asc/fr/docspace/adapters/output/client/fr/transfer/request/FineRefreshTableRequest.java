package com.asc.fr.docspace.adapters.output.client.fr.transfer.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class FineRefreshTableRequest {
  @JsonProperty("id")
  private String id;

  /** 3 = Excel / ask Spider to re-extract. */
  @JsonProperty("type")
  private int type = 3;

  @JsonProperty("packageId")
  private String packageId;

  public static FineRefreshTableRequest of(String tableUuid, String folderId) {
    return new FineRefreshTableRequest(tableUuid, 3, folderId);
  }
}

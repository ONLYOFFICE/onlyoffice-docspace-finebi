package com.asc.fr.docspace.adapters.output.client.fr.transfer.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Body of {@code POST /v5/conf/excel/sheet/preview} (RESET flow, step 1). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineSheetPreviewRequest {
  @JsonProperty("attachId")
  private String attachId;

  @JsonProperty("sheetIndex")
  private int sheetIndex;

  @JsonProperty("tableBean")
  private FineTableBeanRequest tableBean;
}

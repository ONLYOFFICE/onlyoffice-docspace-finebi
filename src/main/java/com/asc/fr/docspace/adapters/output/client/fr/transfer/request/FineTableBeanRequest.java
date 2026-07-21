package com.asc.fr.docspace.adapters.output.client.fr.transfer.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineTableBeanRequest {
  @JsonProperty("name")
  private String name;

  @JsonProperty("transferName")
  private String transferName;

  @JsonProperty("parentId")
  private String parentId;

  /** 3 = Excel dataset. */
  @JsonProperty("type")
  private int type = 3;

  @JsonProperty("engineType")
  private String engineType = "spider";

  /** 1 = new upload, 2 = RESET (replace source of an existing table). */
  @JsonProperty("uploadType")
  private int uploadType;

  @JsonProperty("additionalAttach")
  private List<Object> additionalAttach = Collections.emptyList();

  @JsonProperty("baseAttach")
  private Object baseAttach;

  @JsonProperty("excelFields")
  private Object excelFields;
}

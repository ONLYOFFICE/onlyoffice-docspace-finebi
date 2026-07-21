package com.asc.fr.docspace.adapters.output.client.fr.transfer.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineExcelAddRequest {
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Table {
    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("tableBean")
    private FineTableBeanRequest tableBean;
  }

  @JsonProperty("excelAddTables")
  private List<Table> excelAddTables = Collections.emptyList();

  @JsonProperty("moduleType")
  private int moduleType = 1;

  @JsonProperty("engineType")
  private String engineType = "spider";
}

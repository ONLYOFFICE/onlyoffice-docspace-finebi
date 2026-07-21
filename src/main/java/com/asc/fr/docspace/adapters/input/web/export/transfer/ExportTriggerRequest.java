package com.asc.fr.docspace.adapters.input.web.export.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExportTriggerRequest {
  private String operationId;
  private String reportName;
}

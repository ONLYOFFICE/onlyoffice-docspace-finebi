package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import com.asc.fr.docspace.adapters.input.web.OkResponse;
import lombok.Getter;

@Getter
public final class ImportResponse extends OkResponse {
  private final String datasetName;

  public ImportResponse(String datasetName) {
    super(true);
    this.datasetName = datasetName;
  }
}

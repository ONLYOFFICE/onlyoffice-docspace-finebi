package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import com.asc.fr.docspace.adapters.input.web.OkResponse;
import lombok.Getter;

@Getter
public final class ImportResponse extends OkResponse {
  /** Number of FineBI datasets created (one per imported sheet). */
  private final int count;

  public ImportResponse(int count) {
    super(true);
    this.count = count;
  }
}

package com.asc.fr.docspace.adapters.input.web.export.transfer;

import com.asc.fr.docspace.adapters.input.web.OkResponse;
import lombok.Getter;

@Getter
public final class ExportConfigResponse extends OkResponse {
  private final boolean configured;
  private final boolean loggedIn;
  private final String tenantUrl;
  private final String exportUrl;
  private final String uploadUrl;

  public ExportConfigResponse(
      boolean configured, boolean loggedIn, String tenantUrl, String exportUrl, String uploadUrl) {
    super(true);
    this.configured = configured;
    this.loggedIn = loggedIn;
    this.tenantUrl = tenantUrl;
    this.exportUrl = exportUrl;
    this.uploadUrl = uploadUrl;
  }
}

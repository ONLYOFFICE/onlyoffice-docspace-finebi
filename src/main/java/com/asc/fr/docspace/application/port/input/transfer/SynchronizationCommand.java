package com.asc.fr.docspace.application.port.input.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SynchronizationCommand implements Command {
  private final String decisionBase;
  private final String fileId;
  @Builder.Default private final String tenantUrl = "";

  public String getTenantUrl() {
    return tenantUrl == null ? "" : tenantUrl;
  }

  @Override
  public boolean valid() {
    if (decisionBase == null || decisionBase.trim().isEmpty()) {
      return false;
    }
    return fileId != null && !fileId.trim().isEmpty();
  }
}

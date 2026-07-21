package com.asc.fr.docspace.application.port.input.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExportFileCommand implements Command {
  private final String userName;
  private final String operationId;
  private final String reportName;

  @Override
  public boolean valid() {
    if (userName == null || userName.trim().isEmpty()) return false;

    if (operationId == null || operationId.trim().isEmpty()) return false;

    return reportName != null && !reportName.trim().isEmpty();
  }
}

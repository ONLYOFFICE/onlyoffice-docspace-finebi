package com.asc.fr.docspace.application.port.output.fr.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FineRefreshDatasetCommand implements Command {
  private final String folderId;
  private final String tableId;

  @Override
  public boolean valid() {
    if (folderId == null || folderId.trim().isEmpty()) return false;

    return tableId != null && !tableId.trim().isEmpty();
  }
}

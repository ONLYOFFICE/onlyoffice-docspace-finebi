package com.asc.fr.docspace.application.port.output.fr.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FineReplaceDatasetCommand implements Command {
  private final String tableId;
  private final String folderId;
  private final String name;
  private final String fileName;
  private final int sheetIndex;

  @Override
  public boolean valid() {
    if (tableId == null || tableId.trim().isEmpty()) return false;

    if (folderId == null || folderId.trim().isEmpty()) return false;

    if (name == null || name.trim().isEmpty()) return false;

    return fileName != null && !fileName.trim().isEmpty();
  }
}

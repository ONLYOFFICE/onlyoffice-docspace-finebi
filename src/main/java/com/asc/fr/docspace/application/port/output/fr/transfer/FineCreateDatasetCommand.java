package com.asc.fr.docspace.application.port.output.fr.transfer;

import com.asc.fr.docspace.application.port.Command;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FineCreateDatasetCommand implements Command {
  private final String tableName;
  private final String folderId;
  private final FineAttachment attachment;

  @Override
  public boolean valid() {
    if (tableName == null || tableName.trim().isEmpty()) return false;

    if (folderId == null || folderId.trim().isEmpty()) return false;

    return attachment != null;
  }
}

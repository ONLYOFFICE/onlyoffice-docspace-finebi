package com.asc.fr.docspace.application.port.output.fr.transfer;

import com.asc.fr.docspace.application.port.Command;
import com.asc.fr.docspace.domain.common.Sheet;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FineCreateDatasetCommand implements Command {
  private final String tableName;
  private final String folderId;
  private final FineAttachment attachment;
  @Builder.Default private final List<Sheet> sheets = Collections.emptyList();
  @Builder.Default private final List<Integer> sheetIndices = Collections.emptyList();

  public List<Sheet> getSheets() {
    return sheets == null ? Collections.emptyList() : sheets;
  }

  public List<Integer> getSheetIndices() {
    return sheetIndices == null ? Collections.emptyList() : sheetIndices;
  }

  @Override
  public boolean valid() {
    if (tableName == null || tableName.trim().isEmpty()) return false;

    if (folderId == null || folderId.trim().isEmpty()) return false;

    return attachment != null;
  }
}

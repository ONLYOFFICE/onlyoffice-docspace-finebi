package com.asc.fr.docspace.domain.common;

import lombok.Value;
import lombok.With;

@Value
public class FileSynchronizationRecord {
  String tableName;
  String folderId;
  @With String tableId;

  public FileSynchronizationRecord(String tableName, String folderId, String tableId) {
    this.tableName = tableName == null ? "" : tableName;
    this.folderId = folderId == null ? "" : folderId;
    this.tableId = tableId == null ? "" : tableId;
  }
}

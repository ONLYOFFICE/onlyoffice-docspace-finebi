package com.asc.fr.docspace.domain.common;

import lombok.Value;
import lombok.With;

@Value
public class FileSynchronizationRecord {
  String fileId;
  String tableName;
  String folderId;
  @With String tableId;
  long lastReconciledAt;

  public FileSynchronizationRecord(
      String fileId, String tableName, String folderId, String tableId) {
    this(fileId, tableName, folderId, tableId, 0L);
  }

  public FileSynchronizationRecord(
      String fileId, String tableName, String folderId, String tableId, long lastReconciledAt) {
    this.fileId = fileId == null ? "" : fileId;
    this.tableName = tableName == null ? "" : tableName;
    this.folderId = folderId == null ? "" : folderId;
    this.tableId = tableId == null ? "" : tableId;
    this.lastReconciledAt = lastReconciledAt;
  }
}

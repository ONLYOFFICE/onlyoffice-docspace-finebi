package com.asc.fr.docspace.domain.common;

import lombok.Value;
import lombok.With;

@Value
public class FileSynchronizationRecord {
  String fileId;
  @With String tableId;
  @With int sheetId;
  @With String contentHash;
  long lastReconciledAt;
  @With String tenantUrl;

  public FileSynchronizationRecord(String fileId, String tableId) {
    this(fileId, tableId, 0, "", 0L, "");
  }

  public FileSynchronizationRecord(String fileId, String tableId, int sheetId) {
    this(fileId, tableId, sheetId, "", 0L, "");
  }

  public FileSynchronizationRecord(
      String fileId, String tableId, int sheetId, long lastReconciledAt) {
    this(fileId, tableId, sheetId, "", lastReconciledAt, "");
  }

  public FileSynchronizationRecord(
      String fileId, String tableId, int sheetId, String contentHash, long lastReconciledAt) {
    this(fileId, tableId, sheetId, contentHash, lastReconciledAt, "");
  }

  public FileSynchronizationRecord(
      String fileId,
      String tableId,
      int sheetId,
      String contentHash,
      long lastReconciledAt,
      String tenantUrl) {
    this.fileId = fileId == null ? "" : fileId;
    this.tableId = tableId == null ? "" : tableId;
    this.sheetId = sheetId;
    this.contentHash = contentHash == null ? "" : contentHash;
    this.lastReconciledAt = lastReconciledAt;
    this.tenantUrl = tenantUrl == null ? "" : tenantUrl;
  }
}

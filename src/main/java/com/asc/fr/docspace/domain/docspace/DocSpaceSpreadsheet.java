package com.asc.fr.docspace.domain.docspace;

import lombok.Getter;

@Getter
public final class DocSpaceSpreadsheet {
  private static final String DEFAULT_FILENAME = "file.xlsx";

  private final String fileName;
  private final String tableName;

  private static String sanitize(String name) {
    String cleaned = name == null ? "" : name.trim();
    if (cleaned.isEmpty()) return DEFAULT_FILENAME;

    cleaned = cleaned.replace('\\', '_').replace('/', '_');
    String lower = cleaned.toLowerCase();
    if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls") && !lower.endsWith(".csv"))
      cleaned = cleaned + ".xlsx";

    return cleaned;
  }

  private static String tableNameFrom(String fileName) {
    int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }

  public DocSpaceSpreadsheet(String name) {
    this.fileName = sanitize(name);
    this.tableName = tableNameFrom(this.fileName);
  }

  public static String toDatasetName(String base, String sheetName) {
    String sheet =
        sheetName == null
            ? ""
            : sheetName.replace('\\', '_').replace('/', '_').trim().replaceAll("\\s+", " ");
    return sheet.isEmpty() ? base : base + "_" + sheet;
  }
}

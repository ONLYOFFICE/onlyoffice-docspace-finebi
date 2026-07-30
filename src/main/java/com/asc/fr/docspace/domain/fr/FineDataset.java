package com.asc.fr.docspace.domain.fr;

import lombok.Value;

@Value
public class FineDataset {
  String sheetName;
  int sheetId;
  String tableName;
  String tableId;
}

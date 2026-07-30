package com.asc.fr.docspace.adapters.output.client.fr.transfer;

import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineSheetPreviewDataResponse;
import lombok.Value;

@Value
public class FineSheetPreview {
  int sheetIndex;
  int sheetId;
  String tableName;
  String sheetName;
  FineSheetPreviewDataResponse data;
}

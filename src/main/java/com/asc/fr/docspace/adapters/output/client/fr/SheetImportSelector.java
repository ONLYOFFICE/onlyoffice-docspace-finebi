package com.asc.fr.docspace.adapters.output.client.fr;

import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineEnvelope;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineSheetPreview;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineSheetPreviewDataResponse;
import com.asc.fr.docspace.domain.common.Sheet;
import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Walks a workbook's preview indices and decides which sheets become datasets.
 *
 * <p>The classification is derived from live behaviour:
 *
 * <ul>
 *   <li><b>success and non-empty {@code fields}</b> - an importable sheet, collected.
 *   <li><b>failure</b> (e.g. {@code FineExcelHeaderException} on a sheet with no usable header) -
 *       the sheet still occupies its index, so we skip it and keep scanning later sheets.
 *   <li><b>success and empty {@code fields}</b> - the index is past the last sheet; scanning stops.
 * </ul>
 *
 * <p>Blank sheets never appear here
 */
final class SheetImportSelector {
  static final int MAX_SHEETS = 512;

  @FunctionalInterface
  interface PreviewExecutor {
    String preview(int sheetIndex, String tableName) throws IOException;
  }

  private SheetImportSelector() {}

  private static String sheetNameLocation(List<Sheet> sheets, int index) {
    if (index < sheets.size()) return sheets.get(index).getName();
    return index == 0 ? "" : "Sheet" + (index + 1);
  }

  private static int sheetIdLocation(List<Sheet> sheets, int index) {
    return index < sheets.size() ? sheets.get(index).getSheetId() : 0;
  }

  private static boolean isAuthFailure(String response) {
    return response != null && response.contains("TokenNotExistException");
  }

  private static boolean hasFields(FineSheetPreviewDataResponse data) {
    if (data == null) return false;

    JsonNode fields = data.getFields();
    return fields != null && fields.isArray() && !fields.isEmpty();
  }

  static List<FineSheetPreview> select(String base, List<Sheet> sheets, PreviewExecutor executor)
      throws IOException {
    List<FineSheetPreview> selected = new ArrayList<>();

    int max = sheets.isEmpty() ? MAX_SHEETS : sheets.size();
    for (int index = 0; index < max; index++) {
      String sheetName = sheetNameLocation(sheets, index);
      int sheetId = sheetIdLocation(sheets, index);
      String tableName = DocSpaceSpreadsheet.toDatasetName(base, sheetName);

      String response = executor.preview(index, tableName);
      FineEnvelope envelope = FineEnvelope.parse(response);

      if (envelope.isSuccess()) {
        FineSheetPreviewDataResponse data = envelope.dataAs(FineSheetPreviewDataResponse.class);
        if (!hasFields(data)) break;

        selected.add(new FineSheetPreview(index, sheetId, tableName, sheetName, data));
      } else if (isAuthFailure(response)) {
        throw new IOException("FineBI sheet preview authentication failed");
      }
      // otherwise: a per-sheet failure (e.g. FineExcelHeaderException) — skip it and keep scanning.
      // Note: a plain success:false is NOT treated as auth failure here, or every bad-header sheet
      // would abort the whole workbook; only an explicit token error stops the walk.
    }

    return selected;
  }
}

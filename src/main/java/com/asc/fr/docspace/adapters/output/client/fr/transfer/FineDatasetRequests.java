package com.asc.fr.docspace.adapters.output.client.fr.transfer;

import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineExcelAddRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineSheetPreviewRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineTableBeanRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineAttachmentResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineSheetPreviewDataResponse;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds FineBI request DTOs for the create / replace dataset flows.
 *
 * <p>Both flows let FineBI parse the uploaded spreadsheet: sheet/preview is asked to resolve {@code
 * baseAttach}/{@code fields} server-side, and those nodes are echoed verbatim into excel/add or
 * tables/update. Every request targets a single {@code sheetIndex}; a multi-sheet workbook is
 * imported by previewing each index in turn and batching the resolved sheets into one excel/add.
 */
public final class FineDatasetRequests {
  private FineDatasetRequests() {}

  private static FineSheetPreviewRequest preview(
      FineTableBeanRequest bean, FineAttachment attachment, int sheetIndex) {
    bean.setBaseAttach(FineAttachmentResponse.emptyPlaceholder());
    bean.setExcelFields(Collections.emptyList());

    FineSheetPreviewRequest preview = new FineSheetPreviewRequest();
    preview.setAttachId(attachment.getAttachId());
    preview.setSheetIndex(sheetIndex);
    preview.setTableBean(bean);

    return preview;
  }

  private static FineTableBeanRequest tableBean(
      String name, String transferName, String parentId, int uploadType) {
    FineTableBeanRequest bean = new FineTableBeanRequest();
    bean.setName(name);
    bean.setTransferName(transferName);
    bean.setParentId(parentId);
    bean.setUploadType(uploadType);

    return bean;
  }

  private static FineTableBeanRequest fromPreview(
      String name, String transferName, String folderId, FineSheetPreviewDataResponse preview) {
    FineTableBeanRequest bean = tableBean(name, transferName, folderId, 1);
    bean.setBaseAttach(preview.getBaseAttach());

    JsonNode fields = preview.getFields();
    bean.setExcelFields(
        fields == null || fields.isMissingNode() || fields.isNull()
            ? Collections.emptyList()
            : fields);

    return bean;
  }

  public static FineSheetPreviewRequest createPreview(
      String tableName, String folderId, FineAttachment attachment, int sheetIndex) {
    return preview(tableBean(tableName, tableName, folderId, 1), attachment, sheetIndex);
  }

  public static FineSheetPreviewRequest resetPreview(
      String tableUuid,
      String tableName,
      String folderId,
      FineAttachment attachment,
      int sheetIndex) {
    return preview(tableBean(tableUuid, tableName, folderId, 2), attachment, sheetIndex);
  }

  public static FineExcelAddRequest excelAdd(String folderId, List<FineSheetPreview> sheets) {
    List<FineExcelAddRequest.Table> tables = new ArrayList<>(sheets.size());
    for (FineSheetPreview sheet : sheets) {
      FineExcelAddRequest.Table tableEntry = new FineExcelAddRequest.Table();
      tableEntry.setTableName(sheet.getTableName());
      tableEntry.setTableBean(
          fromPreview(sheet.getTableName(), sheet.getTableName(), folderId, sheet.getData()));
      tables.add(tableEntry);
    }

    FineExcelAddRequest request = new FineExcelAddRequest();
    request.setExcelAddTables(tables);
    return request;
  }

  public static FineTableBeanRequest updateAfterReset(
      String tableUuid, String tableName, String folderId, FineSheetPreviewDataResponse preview) {
    return fromPreview(tableUuid, tableName, folderId, preview);
  }
}

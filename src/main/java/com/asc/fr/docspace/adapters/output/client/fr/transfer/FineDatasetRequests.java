package com.asc.fr.docspace.adapters.output.client.fr.transfer;

import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineExcelAddRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineSheetPreviewRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineTableBeanRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineAttachmentResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineSheetPreviewDataResponse;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;

/**
 * Builds FineBI request DTOs for the create / replace dataset flows.
 *
 * <p>Both flows let FineBI parse the uploaded spreadsheet: sheet/preview is asked to resolve {@code
 * baseAttach}/{@code fields} server-side, and those nodes are echoed verbatim into excel/add or
 * tables/update.
 */
public final class FineDatasetRequests {
  private FineDatasetRequests() {}

  private static FineSheetPreviewRequest preview(
      FineTableBeanRequest bean, FineAttachment attachment) {
    bean.setBaseAttach(FineAttachmentResponse.emptyPlaceholder());
    bean.setExcelFields(Collections.emptyList());

    FineSheetPreviewRequest preview = new FineSheetPreviewRequest();
    preview.setAttachId(attachment.getAttachId());
    preview.setSheetIndex(0);
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

  /** Preview for a brand-new table (create flow, step 1). */
  public static FineSheetPreviewRequest createPreview(
      String tableName, String folderId, FineAttachment attachment) {
    return preview(tableBean(tableName, tableName, folderId, 1), attachment);
  }

  /** Preview that RESETs the source of an existing table (replace flow, step 1). */
  public static FineSheetPreviewRequest resetPreview(
      String tableUuid, String tableName, String folderId, FineAttachment attachment) {
    return preview(tableBean(tableUuid, tableName, folderId, 2), attachment);
  }

  /** excel/add body from the FineBI-resolved preview (create flow, step 2). */
  public static FineExcelAddRequest excelAdd(
      String tableName, String folderId, FineSheetPreviewDataResponse preview) {
    FineExcelAddRequest.Table tableEntry = new FineExcelAddRequest.Table();
    tableEntry.setTableName(tableName);
    tableEntry.setTableBean(fromPreview(tableName, tableName, folderId, preview));

    FineExcelAddRequest request = new FineExcelAddRequest();
    request.setExcelAddTables(Collections.singletonList(tableEntry));
    return request;
  }

  /** tables/update body from the RESET preview (replace flow, step 2). */
  public static FineTableBeanRequest updateAfterReset(
      String tableUuid, String tableName, String folderId, FineSheetPreviewDataResponse preview) {
    return fromPreview(tableUuid, tableName, folderId, preview);
  }
}

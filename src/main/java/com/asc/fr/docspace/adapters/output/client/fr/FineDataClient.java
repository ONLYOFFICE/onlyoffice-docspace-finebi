package com.asc.fr.docspace.adapters.output.client.fr;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineDatasetRequests;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineEnvelope;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineResponses;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineSheetPreview;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineCreateFolderRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineRefreshTableRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineSheetPreviewRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineAttachmentResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineFolderIdDataResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineSheetPreviewDataResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineTableSummaryResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.exception.DatasetAbsentException;
import com.asc.fr.docspace.application.exception.ImportRejectedException;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineDatasetLocation;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceOutcome;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineUploadAttachmentCommand;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineDataset;
import com.asc.fr.docspace.domain.fr.FineFolder;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

@RequiredArgsConstructor
public final class FineDataClient
    implements FineAttachmentService, FineFolderService, FineDatasetService {
  private static final MediaType CSV = MediaType.parse("text/csv");
  private static final MediaType XLSX =
      MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private static final FineMapper MAPPER = FineMapper.INSTANCE;

  private final FineRest rest;

  private static String firstNonEmpty(String a, String b) {
    return !Strings.isNullOrEmpty(a) ? a : Strings.nullToEmpty(b);
  }

  private static String token(FineSession session) {
    return Strings.emptyToNull(session.getAuthToken());
  }

  private String post(FineSession session, String path, Object body) throws IOException {
    return Calls.string(
        rest.postJson(session.getBaseUrl() + path, token(session), session.getCookie(), body));
  }

  private String createFolder(FineSession session, String name) throws IOException {
    String resp =
        post(
            session,
            Paths.PACKS_FOLDERS.path(),
            new FineCreateFolderRequest(name, Paths.ROOT_GROUP_ID));
    FineEnvelope envelope =
        FineEnvelope.parse(resp).requireNotFailed("FineBI folder creation failed");
    FineFolderIdDataResponse data = envelope.dataAs(FineFolderIdDataResponse.class);
    String id = firstNonEmpty(data == null ? null : data.getId(), envelope.getId());
    if (id.isEmpty()) throw new IOException("FineBI folder created but no id returned");

    return id;
  }

  private FineSheetPreviewDataResponse sheetPreview(
      FineSession session, FineSheetPreviewRequest request, String absentTableId)
      throws IOException {
    String response = post(session, Paths.SHEET_PREVIEW.path(), request);
    FineEnvelope envelope = FineEnvelope.parse(response);
    if (absentTableId != null && envelope.tableAbsent())
      throw new DatasetAbsentException(absentTableId);

    envelope.requireSuccess("FineBI sheet preview failed");
    FineSheetPreviewDataResponse preview = envelope.dataAs(FineSheetPreviewDataResponse.class);
    if (preview == null
        || preview.getBaseAttach() == null
        || preview.getBaseAttach().isMissingNode()
        || preview.getBaseAttach().isNull())
      throw new IOException("FineBI sheet preview returned no base attachment");

    return preview;
  }

  private static Set<String> parseTableIds(String body) {
    try {
      FineEnvelope envelope = FineEnvelope.parse(body);
      if (envelope.authFailed(body))
        throw new CompletionException(
            new IOException("FineBI dataset existence probe authentication failed"));
      if (envelope.tableAbsent()) return Collections.emptySet();
      if (envelope.isExplicitFailure())
        throw new CompletionException(new IOException("FineBI pack tables probe failed"));
      try {
        return FineResponses.tableIds(Json.MAPPER.readTree(body));
      } catch (Exception e) {
        throw new CompletionException(new IOException("FineBI pack tables json failed", e));
      }
    } catch (IOException e) {
      throw new CompletionException(e);
    }
  }

  private FineSheetPreviewDataResponse previewSheet(
      FineSession session,
      FineAttachment attachment,
      String tableId,
      String folderId,
      int sheetIndex)
      throws IOException {
    String probeName = "docspace-resync-" + tableId + "-" + sheetIndex;
    return sheetPreview(
        session,
        FineDatasetRequests.createPreview(probeName, folderId, attachment, sheetIndex),
        null);
  }

  @Override
  public FineAttachment uploadAttachment(FineUploadAttachmentCommand command, FineSession session)
      throws IOException {
    String filename = command.getFileName();
    byte[] content = command.getContent();
    String url = session.getBaseUrl() + Paths.ATTACH_UPLOAD.path() + "?width=32&height=32";
    MediaType mediaType = filename.toLowerCase().endsWith(".csv") ? CSV : XLSX;
    MultipartBody.Part part =
        MultipartBody.Part.createFormData(
            "FileData", filename, RequestBody.create(mediaType, content));

    FineEnvelope envelope =
        FineEnvelope.parse(
            Calls.string(
                rest.uploadAttachment(url, filename, token(session), session.getCookie(), part)));
    FineAttachmentResponse data = envelope.dataAs(FineAttachmentResponse.class);
    String attachId =
        firstNonEmpty(data == null ? null : data.getAttachId(), envelope.getAttachId());
    if (attachId.isEmpty()) throw new IOException("FineBI attach upload returned no attachment id");

    FineAttachmentResponse resolved = data == null ? new FineAttachmentResponse() : data;
    resolved.setAttachId(attachId);
    return MAPPER.toAttachment(resolved);
  }

  @Override
  public List<FineFolder> listFolders(FineSession session) throws IOException {
    String body =
        Calls.string(
            rest.get(
                session.getBaseUrl() + Paths.PACKS_FOLDERS.path(),
                token(session),
                session.getCookie()));
    FineEnvelope envelope = FineEnvelope.parse(body);
    FineResponses.requireValidAuthentication(body, envelope);
    return FineResponses.foldersFromPacks(envelope);
  }

  @Override
  public String ensureFolder(String name, FineSession session) throws IOException {
    for (FineFolder folder : listFolders(session))
      if (name.equals(folder.getName())) return folder.getId();

    return createFolder(session, name);
  }

  @Override
  public List<FineDataset> createDatasets(FineCreateDatasetCommand command, FineSession session)
      throws IOException {
    String base = command.getTableName();
    String folderId = command.getFolderId();
    FineAttachment attachment = command.getAttachment();

    List<FineSheetPreview> sheets =
        SheetImportSelector.select(
            base,
            command.getSheets(),
            (sheetIndex, tableName) ->
                post(
                    session,
                    Paths.SHEET_PREVIEW.path(),
                    FineDatasetRequests.createPreview(
                        tableName, folderId, attachment, sheetIndex)));

    if (sheets.isEmpty())
      throw new ImportRejectedException(
          "import.error.noImportableSheets", "The workbook has no sheet FineBI can import");

    String response =
        post(session, Paths.TABLE_ADD.path(), FineDatasetRequests.excelAdd(folderId, sheets));

    FineEnvelope envelope = FineEnvelope.parse(response).requireSuccess("FineBI table add failed");
    Map<String, String> datasetUUID = FineResponses.getDatasetUUID(envelope);
    List<FineDataset> created = new ArrayList<>(sheets.size());

    for (FineSheetPreview sheet : sheets) {
      String uuid = datasetUUID.get(sheet.getTableName());
      if ((uuid == null || uuid.isEmpty()) && sheets.size() == 1)
        uuid = FineResponses.datasetUuid(envelope, sheet.getTableName());
      if (uuid == null || uuid.isEmpty()) continue;
      created.add(
          new FineDataset(sheet.getSheetName(), sheet.getSheetId(), sheet.getTableName(), uuid));
    }

    if (created.isEmpty()) throw new IOException("FineBI created no datasets from the workbook");

    return created;
  }

  @Override
  public Map<String, FineDatasetLocation> locateDatasets(
      Collection<String> tableIds, FineSession session) throws IOException {
    Map<String, FineDatasetLocation> located = new HashMap<>();

    if (tableIds == null || tableIds.isEmpty()) return located;

    Set<String> remaining = new HashSet<>(tableIds);
    remaining.remove(null);
    remaining.remove("");

    for (FineFolder folder : listFolders(session)) {
      if (remaining.isEmpty()) break;
      JsonNode tables;

      try {
        tables =
            Json.MAPPER.readTree(
                Calls.string(
                    rest.get(
                        session.getBaseUrl() + Paths.PACK_TABLES.path(folder.getId()),
                        token(session),
                        session.getCookie())));
      } catch (Exception folderUnreadable) {
        continue;
      }

      for (String tableId : new ArrayList<>(remaining)) {
        FineTableSummaryResponse table = FineResponses.findTable(tables, tableId, "");
        if (table != null) {
          located.put(
              tableId,
              new FineDatasetLocation(
                  folder.getId(), Strings.nullToEmpty(table.getTransferName())));
          remaining.remove(tableId);
        }
      }
    }

    return located;
  }

  @Override
  public List<FineReplaceOutcome> replaceDatasets(
      List<FineReplaceDatasetCommand> commands, FineSession session, FineAttachment attachment) {
    if (commands == null || commands.isEmpty()) return Collections.emptyList();

    List<FineReplaceOutcome> outcomes = new ArrayList<>(commands.size());
    for (FineReplaceDatasetCommand command : commands) {
      String tableId = command.getTableId();
      try {
        String displayName = command.getName();
        if (displayName == null || displayName.isEmpty()) {
          outcomes.add(FineReplaceOutcome.failed(tableId, "could not resolve FineBI dataset name"));
          continue;
        }

        FineSheetPreviewDataResponse preview =
            previewSheet(
                session, attachment, tableId, command.getFolderId(), command.getSheetIndex());

        String updateResp =
            post(
                session,
                Paths.TABLE_UPDATE.path(),
                FineDatasetRequests.updateAfterReset(
                    tableId, displayName, command.getFolderId(), preview));
        FineEnvelope updateEnvelope = FineEnvelope.parse(updateResp);

        if (updateEnvelope.tableAbsent()) {
          outcomes.add(FineReplaceOutcome.absent(tableId));
          continue;
        }

        updateEnvelope.requireSuccess("FineBI table update failed");
        outcomes.add(FineReplaceOutcome.replaced(tableId, displayName));
      } catch (DatasetAbsentException absent) {
        outcomes.add(FineReplaceOutcome.absent(tableId));
      } catch (IOException failed) {
        outcomes.add(FineReplaceOutcome.failed(tableId, failed.getMessage()));
      }
    }

    return outcomes;
  }

  @Override
  public void refreshDataset(FineRefreshDatasetCommand command, FineSession session) {
    try {
      post(
          session,
          Paths.TABLE_UPDATE.path(),
          FineRefreshTableRequest.of(command.getTableId(), command.getFolderId()));
    } catch (Exception ignored) {
      // Best-effort Spider refresh — failures must not fail the replace flow.
      // TODO: Handle exception somehow? UI event?
    }
  }
}

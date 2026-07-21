package com.asc.fr.docspace.adapters.output.client.fr;

import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineDatasetRequests;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineEnvelope;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineResponses;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineCreateFolderRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineRefreshTableRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.request.FineSheetPreviewRequest;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineAttachmentResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineFolderIdDataResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineSheetPreviewDataResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.exception.DatasetAbsentException;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineUploadAttachmentCommand;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineFolder;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.List;
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
  public String createDataset(FineCreateDatasetCommand command, FineSession session)
      throws IOException {
    String tableName = command.getTableName();
    String folderId = command.getFolderId();
    FineSheetPreviewDataResponse preview =
        sheetPreview(
            session,
            FineDatasetRequests.createPreview(tableName, folderId, command.getAttachment()),
            null);

    String response =
        post(
            session,
            Paths.TABLE_ADD.path(),
            FineDatasetRequests.excelAdd(tableName, folderId, preview));
    FineEnvelope envelope = FineEnvelope.parse(response).requireSuccess("FineBI table add failed");
    return FineResponses.datasetUuid(envelope, tableName);
  }

  @Override
  public void replaceDataset(
      FineReplaceDatasetCommand command, FineSession session, FineAttachment attachment)
      throws IOException {
    String tableId = command.getTableId();
    String tableName = command.getTableName();
    String folderId = command.getFolderId();
    FineSheetPreviewDataResponse preview =
        sheetPreview(
            session,
            FineDatasetRequests.resetPreview(tableId, tableName, folderId, attachment),
            tableId);

    String updateResp =
        post(
            session,
            Paths.TABLE_UPDATE.path(),
            FineDatasetRequests.updateAfterReset(tableId, tableName, folderId, preview));
    FineEnvelope updateEnvelope = FineEnvelope.parse(updateResp);
    if (updateEnvelope.tableAbsent()) throw new DatasetAbsentException(tableId);

    updateEnvelope.requireSuccess("FineBI table update failed");
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

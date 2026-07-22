package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.DocSpaceEnvelope;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceFileResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.adapters.output.client.http.RedirectingDownloader;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileRetrievalService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileUploadService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpacePathService;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileCommand;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileFromUrlCommand;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceUploadFileCommand;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

@RequiredArgsConstructor
public final class DocSpaceFileClient
    implements DocSpaceFileUploadService,
        DocSpaceFileDownloadService,
        DocSpaceFileRetrievalService {
  private static final int MAX_UPLOAD_BYTES = PluginManifest.get().limits.docSpaceUploadBytes;
  private static final int MAX_DOWNLOAD_BYTES = PluginManifest.get().limits.docSpaceDownloadBytes;
  private static final String DEFAULT_FILENAME = "file";
  private static final MediaType XLSX =
      MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private static final DocSpaceMapper MAPPER = DocSpaceMapper.INSTANCE;

  private final DocSpaceRest rest;
  private final DocSpaceAuthenticator authenticationClient;
  private final DocSpacePathService pathService;
  private final RedirectingDownloader downloader;

  private byte[] fetch(String url, Map<String, String> headers) throws IOException {
    return downloader.fetch(url, headers, true, MAX_DOWNLOAD_BYTES).bytes;
  }

  private DocSpaceFileResponse fileMeta(String base, String id, String bearer) throws IOException {
    DocSpaceFileResponse meta =
        DocSpaceEnvelope.fileFrom(
            DocSpaceEnvelope.responseOf(Calls.body(rest.fileMeta(base + Paths.file(id), bearer))));
    return meta == null ? new DocSpaceFileResponse() : meta;
  }

  private String downloadUrl(String base, String id, String bearer, DocSpaceFileResponse meta)
      throws IOException {
    String presigned =
        Strings.nullToEmpty(
            DocSpaceEnvelope.responseOf(
                Calls.body(rest.presignedUri(base + Paths.filePresignedUri(id), bearer))));
    if (!presigned.isEmpty()) return presigned;

    String viewUrl = Strings.nullToEmpty(meta.getViewUrl());
    if (viewUrl.isEmpty())
      throw new IOException("DocSpace returned no download URL for file " + id);

    return pathService.absolutize(base, viewUrl);
  }

  @Override
  public boolean fileExists(URL docSpaceUrl, String fileId, DocSpaceAccountCredentials credentials)
      throws IOException {
    if (Strings.isNullOrEmpty(fileId)) return false;

    String base = docSpaceUrl.getValue();
    String bearer = authenticationClient.bearer(docSpaceUrl, credentials);
    Response<DocSpaceEnvelope<JsonNode>> response =
        rest.fileMeta(base + Paths.file(fileId.trim()), bearer).execute();

    if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) return false;
    if (response.isSuccessful()) return true;

    throw new IOException(
        "DocSpace file existence check for " + fileId + " failed with status " + response.code());
  }

  @Override
  public DocSpaceUploadedFile upload(
      DocSpaceUploadFileCommand command, DocSpaceAccountCredentials credentials)
      throws IOException {
    byte[] content = command.getContent();
    if (content == null || content.length == 0) throw new IOException("Export file is empty");

    if (content.length > MAX_UPLOAD_BYTES)
      throw new IOException(
          "Export file exceeds the " + MAX_UPLOAD_BYTES / (1024 * 1024) + " MB limit");

    URL docSpaceUrl = command.getDocSpaceUrl();
    String base = docSpaceUrl.getValue();
    String bearer = authenticationClient.bearer(docSpaceUrl, credentials);
    String folderId = command.getFolderId();
    String targetFolder = Strings.isNullOrEmpty(folderId) ? "@my" : folderId.trim();
    String safeName = pathService.sanitizeFileName(command.getFileName());

    MultipartBody.Part part =
        MultipartBody.Part.createFormData("file", safeName, RequestBody.create(XLSX, content));
    DocSpaceFileResponse response =
        DocSpaceEnvelope.fileFrom(
            DocSpaceEnvelope.responseOf(
                Calls.body(rest.upload(base + Paths.folderUpload(targetFolder), bearer, part))));

    if (response == null) response = new DocSpaceFileResponse();
    if (response.getId() == null && response.getFile() != null) response = response.getFile();

    return MAPPER.toUploadedFile(response, safeName, targetFolder);
  }

  @Override
  public DocSpaceRawFile downloadFromUrl(
      DocSpaceDownloadFileFromUrlCommand command, DocSpaceAccountCredentials credentials)
      throws IOException {
    String viewUrl = command.getViewUrl();
    if (Strings.isNullOrEmpty(viewUrl))
      throw new IOException("View url is required for DocSpace download");

    URL docSpaceUrl = command.getDocSpaceUrl();
    String base = docSpaceUrl.getValue();
    String token = authenticationClient.authenticate(docSpaceUrl, credentials);
    byte[] content =
        fetch(
            pathService.absolutize(base, viewUrl.trim()),
            ImmutableMap.of("Accept", "*/*", "Cookie", "asc_auth_key=" + token));
    if (content.length == 0) throw new IOException("DocSpace returned empty content");

    String filenameHint = command.getFileNameHint();
    String filename = Strings.isNullOrEmpty(filenameHint) ? DEFAULT_FILENAME : filenameHint.trim();
    return MAPPER.toDownloadedFile(filename, content);
  }

  @Override
  public DocSpaceRawFile download(
      DocSpaceDownloadFileCommand command, DocSpaceAccountCredentials credentials)
      throws IOException {
    String fileId = command.getFileId();
    if (Strings.isNullOrEmpty(fileId))
      throw new IOException("File id is required for DocSpace download");

    String id = fileId.trim();
    URL docSpaceUrl = command.getDocSpaceUrl();
    String base = docSpaceUrl.getValue();
    String bearer = authenticationClient.bearer(docSpaceUrl, credentials);

    DocSpaceFileResponse meta = fileMeta(base, id, bearer);
    String filename = Strings.isNullOrEmpty(meta.getTitle()) ? DEFAULT_FILENAME : meta.getTitle();
    byte[] content =
        fetch(
            downloadUrl(base, id, bearer, meta),
            ImmutableMap.of("Accept", "*/*", "Authorization", bearer));
    if (content.length == 0)
      throw new IOException("DocSpace returned empty content for file " + id);

    return MAPPER.toDownloadedFile(filename, content);
  }
}

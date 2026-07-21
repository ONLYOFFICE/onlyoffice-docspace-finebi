package com.asc.fr.docspace.adapters.output.client.fr;

import com.asc.fr.docspace.adapters.output.client.http.RedirectingDownloader;
import com.asc.fr.docspace.application.port.output.fr.FineExportService;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Downloads the Excel file FineBI generated for an export operation ({@code GET
 * /v5/design/report/data/export/download/{operationId}}), following redirects and rejecting
 * FineBI's JSON/HTML error envelopes.
 */
@RequiredArgsConstructor
public final class FineExportClient implements FineExportService {
  private final RedirectingDownloader downloader;

  // TODO: Test file size first (to avoid memory pollution)
  @Override
  public byte[] downloadExport(String operationId, FineSession session) throws IOException {
    String url =
        session.getBaseUrl()
            + "/v5/design/report/data/export/download/"
            + URLEncoder.encode(operationId, StandardCharsets.UTF_8.name());

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, */*");
    headers.put("Cookie", session.getCookie());

    RedirectingDownloader.Downloaded result = downloader.fetch(url, headers, false);
    boolean isError =
        result.contentType.contains("application/json")
            || result.contentType.contains("text/plain")
            || result.contentType.contains("text/html");
    if (result.status != 200 || isError)
      throw new IOException("Could not download an exported file");

    return result.bytes;
  }
}

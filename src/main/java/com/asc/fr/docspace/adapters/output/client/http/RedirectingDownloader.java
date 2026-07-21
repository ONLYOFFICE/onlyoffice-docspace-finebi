package com.asc.fr.docspace.adapters.output.client.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches raw bytes while following redirects manually (the shared clients have auto-redirect
 * disabled so callers can inspect login/auth redirects). This is the one place hand-rolled OkHttp
 * survives, because the redirect handling is too bespoke for Retrofit: FineBI export downloads
 * re-send the session cookie on every hop, while DocSpace presigned downloads must drop credentials
 * when a CDN redirect crosses hosts.
 */
// TODO: Make sure that there is no bytes memory pollution
@RequiredArgsConstructor
public final class RedirectingDownloader {
  private static final int MAX_REDIRECTS = 5;

  private final OkHttpClient http;

  public static final class Downloaded {
    public final int status;
    public final byte[] bytes;
    public final String contentType;

    Downloaded(int status, byte[] bytes, String contentType) {
      this.status = status;
      this.bytes = bytes;
      this.contentType = contentType == null ? "" : contentType;
    }
  }

  private static boolean isRedirect(int status) {
    return status >= 300 && status < 400;
  }

  private static String resolveRedirect(
      String currentUrl, Response response, Map<String, String> headers, boolean stripOnNewHost)
      throws IOException {
    String location = response.header("Location");
    if (location == null || location.isEmpty())
      throw new IOException("Redirect with no Location header. Status code " + response.code());

    HttpUrl base = HttpUrl.parse(currentUrl);
    if (base == null) throw new IOException("Invalid redirect base url: " + currentUrl);

    HttpUrl resolved = base.resolve(location);
    if (resolved == null) throw new IOException("Cannot resolve redirect: " + location);

    if (stripOnNewHost && !resolved.host().equalsIgnoreCase(base.host())) {
      headers.remove("Authorization");
      headers.remove("Cookie");
    }

    return resolved.toString();
  }

  /**
   * @param url the starting URL
   * @param headers initial request headers (e.g. Accept, Authorization, Cookie)
   * @param stripOnNewHost when true, Authorization/Cookie are dropped once a redirect resolves to a
   *     different host
   */
  public Downloaded fetch(String url, Map<String, String> headers, boolean stripOnNewHost)
      throws IOException {
    String currentUrl = url;
    Map<String, String> currentHeaders = new LinkedHashMap<>(headers);

    for (int hop = 0; hop < MAX_REDIRECTS; hop++) {
      Request.Builder builder = new Request.Builder().url(currentUrl).get();
      for (Map.Entry<String, String> header : currentHeaders.entrySet())
        if (header.getValue() != null && !header.getValue().isEmpty())
          builder.header(header.getKey(), header.getValue());

      try (Response response = http.newCall(builder.build()).execute()) {
        if (isRedirect(response.code())) {
          currentUrl = resolveRedirect(currentUrl, response, currentHeaders, stripOnNewHost);
          continue;
        }

        ResponseBody body = response.body();
        byte[] bytes = body == null ? new byte[0] : body.bytes();
        if (response.code() >= 400)
          throw new IOException("Download failed with status code " + response.code());

        return new Downloaded(response.code(), bytes, response.header("Content-Type"));
      }
    }

    throw new IOException("Too many redirects downloading from " + url);
  }
}

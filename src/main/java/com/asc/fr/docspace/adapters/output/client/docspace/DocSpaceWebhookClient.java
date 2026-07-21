package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookCreateRequest;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import okhttp3.ResponseBody;

@RequiredArgsConstructor
public final class DocSpaceWebhookClient implements WebhookRegistrar {
  private final DocSpaceRest rest;
  private final DocSpaceAuthenticator authenticator;

  private boolean alreadyRegistered(String base, String bearer, String callbackUrl)
      throws IOException {
    try (ResponseBody body = Calls.body(rest.listWebhooks(base + Paths.WEBHOOK.path(), bearer))) {
      return body != null && body.string().contains(callbackUrl);
    }
  }

  @Override
  public void register(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials)
      throws IOException {
    String base = docSpaceUrl.getValue();
    String callback = callbackUrl.getValue();
    String bearer = authenticator.bearer(docSpaceUrl, credentials);
    if (alreadyRegistered(base, bearer, callback)) return;

    Calls.verify(
        rest.createWebhook(
            base + Paths.WEBHOOK.path(),
            bearer,
            DocSpaceWebhookCreateRequest.of(callback, secret)));
  }

  @Override
  public void ensureRegistered(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials) {
    if (docSpaceUrl == null
        || callbackUrl == null
        || docSpaceUrl.getValue().isEmpty()
        || callbackUrl.getValue().isEmpty()
        || credentials == null
        || !credentials.isComplete()) return;

    try {
      register(docSpaceUrl, callbackUrl, secret, credentials);
    } catch (IOException ignored) {
      // Opportunistic ensure — never block callers on registration failure.
      // TODO: Handle it somehow. Inform admins via UI event?
    }
  }
}

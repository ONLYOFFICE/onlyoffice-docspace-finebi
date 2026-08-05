package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.DocSpaceEnvelope;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookCreateRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookUpdateRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceWebhookConfigResponse;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceWebhookEntryResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DocSpaceWebhookClient implements WebhookRegistrar {
  private static final String WEBHOOK_NAME = "FineBI Sync";

  private final DocSpaceRest rest;
  private final DocSpaceAuthenticator authenticator;

  private Integer findWebhookId(String base, String bearer, String callbackUrl) throws IOException {
    DocSpaceEnvelope<List<DocSpaceWebhookEntryResponse>> envelope =
        Calls.body(rest.listWebhooks(base + Paths.WEBHOOK.path(), bearer));
    List<DocSpaceWebhookEntryResponse> entries = DocSpaceEnvelope.responseOf(envelope);
    if (entries == null) return null;

    Integer byName = null;
    for (DocSpaceWebhookEntryResponse entry : entries) {
      DocSpaceWebhookConfigResponse config = entry == null ? null : entry.getConfigs();
      if (config == null) continue;

      if (callbackUrl.equals(config.getUri())) return config.getId();
      if (byName == null && WEBHOOK_NAME.equals(config.getName())) byName = config.getId();
    }

    return byName;
  }

  @Override
  public void register(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials)
      throws IOException {
    String base = docSpaceUrl.getValue();
    String callback = callbackUrl.getValue();
    String bearer = authenticator.bearer(docSpaceUrl, credentials);
    Integer id = findWebhookId(base, bearer, callback);

    if (id != null) {
      Calls.verify(
          rest.updateWebhook(
              base + Paths.WEBHOOK.path(),
              bearer,
              DocSpaceWebhookUpdateRequest.of(id, callback, secret)));
      return;
    }

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

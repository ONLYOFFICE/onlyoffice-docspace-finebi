package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookCreateRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookUpdateRequest;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DocSpaceWebhookClient implements WebhookRegistrar {
  private static final String WEBHOOK_NAME = "FineBI Sync";

  private final DocSpaceRest rest;
  private final DocSpaceAuthenticator authenticator;

  /**
   * Finds our FineBI Sync subscription id, if any.
   *
   * <p>DocSpace's list payload is {@code [{ configs: { id, name, uri, ... }, status }]} — the id
   * lives under {@code configs}, but we also accept a few flat shapes. Match is a substring check
   * against the callback URL or the fixed webhook name, since DocSpace never returns the secret and
   * field nesting has varied across versions.
   */
  private Integer findWebhookId(String base, String bearer, String callbackUrl) throws IOException {
    String raw = Calls.string(rest.listWebhooks(base + Paths.WEBHOOK.path(), bearer));
    if (raw.isEmpty()) return null;

    JsonNode root = Json.parse(raw);
    JsonNode list = root.path("response");
    if (!list.isArray()) list = root.isArray() ? root : null;
    if (list == null)
      return null;

    for (JsonNode entry : list) {
      String text = entry.toString();
      if (!text.contains(callbackUrl) && !text.contains(WEBHOOK_NAME)) continue;

      JsonNode config = entry.has("configs") ? entry.get("configs") : entry;
      JsonNode id = config.has("id") ? config.get("id") : entry.get("id");
      if (id != null && id.isNumber()) return id.intValue();
      if (id != null && id.isTextual()) {
        try {
          return Integer.parseInt(id.asText());
        } catch (NumberFormatException ignored) {
          return null;
        }
      }
    }

    return null;
  }

  private void sync(
      URL docSpaceUrl,
      URL callbackUrl,
      String secret,
      DocSpaceAccountCredentials credentials,
      boolean overwriteSecret)
      throws IOException {
    String base = docSpaceUrl.getValue();
    String callback = callbackUrl.getValue();
    String bearer = authenticator.bearer(docSpaceUrl, credentials);
    Integer id = findWebhookId(base, bearer, callback);

    if (id != null) {
      if (!overwriteSecret)
        return;
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

  private boolean usable(URL docSpaceUrl, URL callbackUrl, DocSpaceAccountCredentials credentials) {
    return docSpaceUrl != null
        && callbackUrl != null
        && !docSpaceUrl.getValue().isEmpty()
        && !callbackUrl.getValue().isEmpty()
        && credentials != null
        && credentials.isComplete();
  }

  @Override
  public void register(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials)
      throws IOException {
    // Active-tenant path (setup / explicit register): blind-overwrite secret/uri/name when the
    // FineBI Sync subscription already exists — DocSpace never returns the stored secret, so
    // there is no drift detection; PUT keeps DocSpace signing with FineBI's key.
    sync(docSpaceUrl, callbackUrl, secret, credentials, true);
  }

  @Override
  public void ensureSynced(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials) {
    if (!usable(docSpaceUrl, callbackUrl, credentials)) return;

    try {
      sync(docSpaceUrl, callbackUrl, secret, credentials, true);
    } catch (IOException ignored) {
      // Opportunistic ensure — never block callers on sync failure.
      // TODO: Handle it somehow. Inform admins via UI event?
    }
  }

  @Override
  public void ensureRegistered(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials) {
    if (!usable(docSpaceUrl, callbackUrl, credentials)) return;

    try {
      // Saved/background tenants: create-if-missing only. Never PUT their secret — that belongs
      // to a different DocSpace connection and must not be overwritten by the active tenant's
      // reconciliation pass.
      sync(docSpaceUrl, callbackUrl, secret, credentials, false);
    } catch (IOException ignored) {
      // Opportunistic ensure — never block callers on registration failure.
      // TODO: Handle it somehow. Inform admins via UI event?
    }
  }
}

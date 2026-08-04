package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.AckHttpHandler;
import com.asc.fr.docspace.adapters.input.web.PluginHttpHandler;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.imports.WebhookSignatureVerifierService;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookEvent;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.port.input.SynchronizationService;
import com.asc.fr.docspace.application.port.input.transfer.SynchronizationCommand;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives DocSpace webhook events; on {@code file.updated} for a file we imported, hands off to
 * {@link SynchronizationService} to refresh the FineBI dataset. Handled deliveries answer 200 so
 * DocSpace does not mark them failed; a bad signature answers 401 and is not processed.
 *
 * <p>Every tenant an admin has ever connected to (see DocSpaceSavedTenantService) keeps its own
 * webhook secret, so an inbound request could come from any of them, not just the currently active
 * one. Its signature is tried against every known secret (current + saved) — the one that matches
 * both authenticates the request and identifies which tenant sent it.
 */
public class WebhookHttpHandler extends PluginHttpHandler {
  private static final String FILE_UPDATED_TRIGGER = "file.updated";
  private static final String SIGNATURE_HEADER = "x-docspace-signature-256";
  private static final int MAX_BODY_BYTES = PluginManifest.get().limits.webhookBodyBytes;

  public static final class HeadHandler extends AckHttpHandler {
    public HeadHandler() {
      super(RequestMethod.HEAD, PluginManifest.get().endpoints.webhookCallback, true);
    }
  }

  public static final class GetHandler extends AckHttpHandler {
    public GetHandler() {
      super(RequestMethod.GET, PluginManifest.get().endpoints.webhookCallback, true);
    }
  }

  /**
   * CLEANUP SEMANTICS (DocSpace side) — the FineBI side lives in DefaultResyncService#reimport.
   * When a tracked file leaves DocSpace (moved to trash or permanently deleted), tracking ends: the
   * registry entry is removed so no future webhook can touch the FineBI dataset. The dataset itself
   * is deliberately KEPT — it may back dashboards, and destroying user data on a remote event would
   * be worse than leaving a stale snapshot. Trash-then-restore therefore requires a re-import to
   * resume syncing (while trashed the file cannot be edited anyway, so no sync is missed). Unknown
   * triggers still fall through to the skip branch.
   */
  private static final Set<String> DELETION_TRIGGERS =
      new HashSet<>(Arrays.asList("file.deleted", "file.trashed"));

  private final SynchronizationLinkRegistry synchronizationLinkRegistry;
  private final SynchronizationSettings synchronizationSettings;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceSavedTenantService savedTenantService;
  private final WebhookSignatureVerifierService signatures;
  private final SynchronizationService resync;

  @Inject
  public WebhookHttpHandler(
      SynchronizationLinkRegistry synchronizationLinkRegistry,
      SynchronizationSettings synchronizationSettings,
      DocSpaceTenantService tenantService,
      DocSpaceSavedTenantService savedTenantService,
      WebhookSignatureVerifierService signatures,
      SynchronizationService resync) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.webhookCallback, true);
    this.synchronizationLinkRegistry = synchronizationLinkRegistry;
    this.synchronizationSettings = synchronizationSettings;
    this.tenantService = tenantService;
    this.savedTenantService = savedTenantService;
    this.signatures = signatures;
    this.resync = resync;
  }

  private Map<String, String> knownSecrets() {
    Map<String, String> secrets = new LinkedHashMap<>();

    DocSpaceTenantConfiguration current = tenantService.load();
    String currentUrl = current.getUrl().getValue();
    if (!currentUrl.isEmpty()) {
      String currentSecret = synchronizationSettings.loadSecret();
      if (!currentSecret.isEmpty()) secrets.put(currentUrl, currentSecret);
    }

    for (DocSpaceSavedTenantConnection saved : savedTenantService.listConnections())
      if (!saved.getWebhookSecret().isEmpty())
        secrets.putIfAbsent(saved.getConfiguration().getUrl().getValue(), saved.getWebhookSecret());

    return secrets;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    byte[] body;
    try {
      body = Requests.body(request, MAX_BODY_BYTES);
    } catch (PluginStatusException e) {
      // TODO: Send some UI notification in v2?
      response.setStatus(e.status());
      return;
    }

    Map<String, String> secrets = knownSecrets();
    String signatureHeader = request.getHeader(SIGNATURE_HEADER);
    String matchedTenantUrl = "";
    boolean verified = secrets.isEmpty();
    if (!verified) {
      for (Map.Entry<String, String> candidate : secrets.entrySet()) {
        if (signatures.verify(body, candidate.getValue(), signatureHeader)) {
          matchedTenantUrl = candidate.getKey();
          verified = true;
          break;
        }
      }
    }

    if (!verified) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    WebhookEvent event = WebhookEvent.parse(new String(body, StandardCharsets.UTF_8));
    if (event.fileId().isEmpty()) return;

    if (DELETION_TRIGGERS.contains(event.trigger())) {
      synchronizationLinkRegistry.removeByFile(event.fileId(), matchedTenantUrl);
      return;
    }

    if (!FILE_UPDATED_TRIGGER.equals(event.trigger())) return;

    response.getWriter().flush();
    resync.schedule(
        SynchronizationCommand.builder()
            .decisionBase(Requests.decisionBase(request))
            .fileId(event.fileId())
            .tenantUrl(matchedTenantUrl)
            .build());
  }
}

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
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives DocSpace webhook events; on {@code file.updated} for a file we imported, hands off to
 * {@link SynchronizationService} to refresh the FineBI dataset. Handled deliveries answer 200 so
 * DocSpace does not mark them failed; a bad signature answers 401 and is not processed.
 */
public class WebhookHttpHandler extends PluginHttpHandler {
  private static final String FILE_UPDATED_TRIGGER = "file.updated";
  private static final String SIGNATURE_HEADER = "x-docspace-signature-256";
  private static final int MAX_BODY_BYTES = 1024 * 1024;

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

  private final com.asc.fr.docspace.domain.SynchronizationService registry;
  private final WebhookSignatureVerifierService signatures;
  private final SynchronizationService resync;

  @Inject
  public WebhookHttpHandler(
      com.asc.fr.docspace.domain.SynchronizationService registry,
      WebhookSignatureVerifierService signatures,
      SynchronizationService resync) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.webhookCallback, true);
    this.registry = registry;
    this.signatures = signatures;
    this.resync = resync;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    byte[] body;
    try {
      body = Requests.body(request, MAX_BODY_BYTES);
    } catch (PluginStatusException e) {
      response.setStatus(e.status());
      return;
    }

    String secret = registry.loadSecret();
    if (!secret.isEmpty()
        && !signatures.verify(body, secret, request.getHeader(SIGNATURE_HEADER))) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    WebhookEvent event = WebhookEvent.parse(new String(body, StandardCharsets.UTF_8));
    if (event.fileId().isEmpty()) return;

    if (DELETION_TRIGGERS.contains(event.trigger())) {
      // File removed in DocSpace, therefore stop tracking it.
      registry.remove(event.fileId());
      return;
    }

    if (!FILE_UPDATED_TRIGGER.equals(event.trigger())) return;

    response.getWriter().flush();
    resync.schedule(
        SynchronizationCommand.builder()
            .decisionBase(Requests.decisionBase(request))
            .fileId(event.fileId())
            .build());
  }
}

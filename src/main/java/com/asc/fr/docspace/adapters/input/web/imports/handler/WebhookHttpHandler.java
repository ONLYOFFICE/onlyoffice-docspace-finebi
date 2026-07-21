package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookEvent;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookSignature;
import com.asc.fr.docspace.application.port.input.SynchronizationService;
import com.asc.fr.docspace.application.port.input.transfer.SynchronizationCommand;
import com.fr.decision.fun.impl.BaseHttpHandler;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives DocSpace webhook events; on {@code file.updated} for a file we imported, hands off to
 * {@link SynchronizationService} to refresh the FineBI dataset. Always answers 200 so DocSpace does
 * not mark the delivery failed.
 */
public class WebhookHttpHandler extends BaseHttpHandler {
  private static final String FILE_UPDATED_TRIGGER = "file.updated";
  private static final String SIGNATURE_HEADER = "x-docspace-signature-256";

  /**
   * DocSpace validates the callback URI with a HEAD request before creating the webhook. Two
   * handlers cover both HEAD and GET so no method goes unhandled.
   */
  // TODO: Extract helper handler classes next to base http handler?
  public static final class HeadHandler extends BaseHttpHandler {
    @Override
    public RequestMethod getMethod() {
      return RequestMethod.HEAD;
    }

    @Override
    public String getPath() {
      return PluginManifest.get().endpoints.webhookCallback;
    }

    @Override
    public boolean isPublic() {
      return true;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
      response.setStatus(HttpServletResponse.SC_OK);
    }
  }

  public static final class GetHandler extends BaseHttpHandler {
    @Override
    public RequestMethod getMethod() {
      return RequestMethod.GET;
    }

    @Override
    public String getPath() {
      return PluginManifest.get().endpoints.webhookCallback;
    }

    @Override
    public boolean isPublic() {
      return true;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
      response.setStatus(HttpServletResponse.SC_OK);
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
      new HashSet<>(Arrays.asList("file.deleted", "file.trashed")); // TODO: Decide on file.trashed

  private final com.asc.fr.docspace.domain.SynchronizationService registry;
  private final SynchronizationService resync;

  @Inject
  public WebhookHttpHandler(
      com.asc.fr.docspace.domain.SynchronizationService registry, SynchronizationService resync) {
    this.registry = registry;
    this.resync = resync;
  }

  // TODO: Extract such body reader into some common module and reuse it accross handlers
  // (preferably use a library)?
  private static byte[] readBody(HttpServletRequest request) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try (InputStream in = request.getInputStream()) {
      byte[] chunk = new byte[4096];
      int n;
      while ((n = in.read(chunk)) >= 0) if (n > 0) buf.write(chunk, 0, n);
    }

    return buf.toByteArray();
  }

  @Override
  public RequestMethod getMethod() {
    return RequestMethod.POST;
  }

  @Override
  public String getPath() {
    return PluginManifest.get().endpoints.webhookCallback;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    byte[] body = readBody(request);
    String secret = registry.loadSecret();
    if (!secret.isEmpty())
      WebhookSignature.verify(body, secret, request.getHeader(SIGNATURE_HEADER));

    WebhookEvent event = WebhookEvent.parse(new String(body, StandardCharsets.UTF_8));
    if (DELETION_TRIGGERS.contains(event.trigger()) && !event.fileId().isEmpty()) {
      // File removed in DocSpace, therefore stop tracking it.
      registry.remove(event.fileId());
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    if (!FILE_UPDATED_TRIGGER.equals(event.trigger())) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    if (event.fileId().isEmpty()) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().flush();
    resync.schedule(
        SynchronizationCommand.builder()
            .decisionBase(Requests.decisionBase(request))
            .fileId(event.fileId())
            .build());
  }
}

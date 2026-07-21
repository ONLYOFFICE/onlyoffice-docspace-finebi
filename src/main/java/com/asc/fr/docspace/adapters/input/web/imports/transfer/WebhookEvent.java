package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import com.asc.fr.docspace.adapters.format.Json;
import com.fasterxml.jackson.databind.JsonNode;

/** The two facts we need from a DocSpace webhook body: what happened and to which file. */
public final class WebhookEvent {
  private final String trigger;
  private final String fileId;

  private WebhookEvent(String trigger, String fileId) {
    this.trigger = trigger;
    this.fileId = fileId;
  }

  private static String scalar(JsonNode node) {
    return node.isValueNode() ? node.asText("").trim() : "";
  }

  public static WebhookEvent parse(String json) {
    JsonNode root = Json.parse(json);
    String trigger = root.path("event").path("trigger").asText("");
    String fileId = scalar(root.path("payload").path("id"));
    if (fileId.isEmpty()) fileId = scalar(root.path("fileId"));
    if (fileId.isEmpty()) fileId = scalar(root.path("id"));
    return new WebhookEvent(trigger, fileId);
  }

  public String trigger() {
    return trigger;
  }

  public String fileId() {
    return fileId;
  }
}

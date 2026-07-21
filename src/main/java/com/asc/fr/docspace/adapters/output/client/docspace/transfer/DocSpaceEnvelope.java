package com.asc.fr.docspace.adapters.output.client.docspace.transfer;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceFileResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DocSpaceEnvelope<T> {
  @JsonProperty("response")
  private T response;

  public static <T> T responseOf(DocSpaceEnvelope<T> envelope) {
    return envelope == null ? null : envelope.getResponse();
  }

  public static DocSpaceFileResponse fileFrom(JsonNode responseNode) {
    if (responseNode == null) return null;

    JsonNode node =
        responseNode.isArray()
            ? (!responseNode.isEmpty() ? responseNode.get(0) : responseNode)
            : responseNode;

    if (node.isMissingNode() || node.isNull()) return null;

    return Json.convert(node, DocSpaceFileResponse.class);
  }
}

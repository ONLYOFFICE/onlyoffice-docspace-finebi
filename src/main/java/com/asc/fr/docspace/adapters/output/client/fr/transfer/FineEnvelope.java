package com.asc.fr.docspace.adapters.output.client.fr.transfer;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.format.Text;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FineEnvelope {
  @JsonProperty("success")
  private JsonNode success;

  @JsonProperty("data")
  private JsonNode data;

  @JsonProperty("errorMsg")
  private String errorMsg;

  @JsonProperty("errorCode")
  private String errorCode;

  @JsonProperty("message")
  private String message;

  /** Some attach/folder responses put identifiers on the root instead of {@code data}. */
  @JsonProperty("attach_id")
  private String attachId;

  @JsonProperty("path")
  private String path;

  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonIgnore private String rawBody = "";

  public static FineEnvelope parse(String body) throws IOException {
    FineEnvelope envelope = Json.read(body, FineEnvelope.class);
    envelope.rawBody = body == null ? "" : body;
    return envelope;
  }

  public <T> T dataAs(Class<T> type) {
    if (data == null || data.isNull() || data.isMissingNode()) return null;

    return Json.convert(data, type);
  }

  public <T> T dataAs(TypeReference<T> type) {
    if (data == null || data.isNull() || data.isMissingNode()) return null;

    return Json.convert(data, type);
  }

  public JsonNode dataNode() {
    return data == null ? Json.MAPPER.missingNode() : data;
  }

  public FineEnvelope requireSuccess(String context) throws IOException {
    if (!isSuccess()) throw new IOException(context + ": " + errorMessage());

    return this;
  }

  public FineEnvelope requireNotFailed(String context) throws IOException {
    if (isExplicitFailure()) throw new IOException(context + ": " + errorMessage());

    return this;
  }

  public boolean isSuccess() {
    if (success == null || success.isNull() || success.isMissingNode()) return false;

    if (success.isBoolean()) return success.booleanValue();

    return "true".equalsIgnoreCase(success.asText(""));
  }

  public boolean isExplicitFailure() {
    if (success == null || success.isNull() || success.isMissingNode()) return false;

    if (success.isBoolean()) return !success.booleanValue();

    return "false".equalsIgnoreCase(success.asText(""));
  }

  public boolean tableAbsent() {
    String code = errorCode == null ? "" : errorCode;
    String msg = errorMsg == null ? "" : errorMsg;
    String detail = msg + " " + (message == null ? "" : message);
    return "61310034".equals(code)
        || detail.contains("FineTableAbsentException")
        || detail.contains("TableAbsentException");
  }

  public boolean authFailed(String raw) {
    return (raw != null && raw.contains("TokenNotExistException")) || isExplicitFailure();
  }

  private String errorMessage() {
    if (errorMsg != null && !errorMsg.isEmpty()) return errorMsg;

    if (message != null && !message.isEmpty()) return message;

    return Text.abbreviate(rawBody, 300);
  }
}

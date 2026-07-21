package com.asc.fr.docspace.adapters.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;

public final class Json {
  public static final ObjectMapper MAPPER =
      new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

  private Json() {}

  public static String write(Object value) throws IOException {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IOException("JSON write failed", e);
    }
  }

  public static <T> T read(String json, Class<T> type) throws IOException {
    return read(json, MAPPER.getTypeFactory().constructType(type));
  }

  public static <T> T read(String json, TypeReference<T> type) throws IOException {
    return read(json, MAPPER.getTypeFactory().constructType(type));
  }

  public static JsonNode parse(String json) {
    if (json == null || json.isEmpty()) return MAPPER.createObjectNode();

    try {
      return MAPPER.readTree(json);
    } catch (Exception e) {
      return MAPPER.createObjectNode();
    }
  }

  public static <T> T convert(Object from, Class<T> type) {
    return MAPPER.convertValue(from, type);
  }

  public static <T> T convert(Object from, TypeReference<T> type) {
    return MAPPER.convertValue(from, type);
  }

  private static <T> T read(String json, JavaType type) throws IOException {
    try {
      return MAPPER.readValue(json == null || json.isEmpty() ? "{}" : json, type);
    } catch (JsonProcessingException e) {
      throw new IOException("JSON read failed", e);
    }
  }
}

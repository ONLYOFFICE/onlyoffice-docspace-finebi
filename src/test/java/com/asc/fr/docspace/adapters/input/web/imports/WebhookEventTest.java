package com.asc.fr.docspace.adapters.input.web.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookEvent;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WebhookEventTest {
  static Stream<Arguments> rootLevelIdFallbackCases() {
    return Stream.of(
        Arguments.of("{\"fileId\":\"abc\"}", "abc"), Arguments.of("{\"id\":77}", "77"));
  }

  @Nested
  class WhenJsonIsWellFormed {
    @Test
    void givenNestedEventAndPayload_whenParsing_thenExtractsTriggerAndFileId() {
      WebhookEvent event =
          WebhookEvent.parse(
              "{\"event\":{\"trigger\":\"file.updated\"},\"payload\":{\"id\":123,\"title\":\"a.xlsx\"}}");
      assertEquals("file.updated", event.trigger());
      assertEquals("123", event.fileId());
    }

    @ParameterizedTest
    @MethodSource(
        "com.asc.fr.docspace.adapters.input.web.imports.WebhookEventTest#rootLevelIdFallbackCases")
    void givenRootLevelId_whenPayloadIdIsAbsent_thenFallsBackToRootId(
        String json, String expected) {
      assertEquals(expected, WebhookEvent.parse(json).fileId());
    }

    @Test
    void givenNonScalarIds_whenParsing_thenReturnsEmptyFileId() {
      WebhookEvent event = WebhookEvent.parse("{\"payload\":{\"id\":{\"nested\":1}},\"id\":[1,2]}");
      assertEquals("", event.fileId());
    }
  }

  @Nested
  class WhenJsonIsMalformed {
    @Test
    void givenMalformedJson_whenParsing_thenReturnsEmptyFields() {
      WebhookEvent event = WebhookEvent.parse("example");
      assertEquals("", event.trigger());
      assertEquals("", event.fileId());
    }
  }
}

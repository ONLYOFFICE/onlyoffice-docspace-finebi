package com.asc.fr.docspace.adapters.input.web.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WebhookEventTest {

  @Nested
  class WhenJsonIsWellFormed {
    @Test
    void givenNestedEventAndPayload_whenParsing_thenExtractsTriggerAndFileId() {
      WebhookEvent event =
          WebhookEvent.parse(
              "{\"event\":{\"trigger\":\"file.updated\"},\"payload\":{\"id\":123,\"title\":\"a.xlsx\"}}");

      assertThat(event.trigger()).isEqualTo("file.updated");
      assertThat(event.fileId()).isEqualTo("123");
    }

    @ParameterizedTest
    @CsvSource({"'{\"fileId\":\"abc\"}', abc", "'{\"id\":77}', 77"})
    void givenRootLevelId_whenPayloadIdIsAbsent_thenFallsBackToRootId(
        String json, String expected) {
      assertThat(WebhookEvent.parse(json).fileId()).isEqualTo(expected);
    }

    @Test
    void givenNonScalarIds_whenParsing_thenReturnsEmptyFileId() {
      WebhookEvent event = WebhookEvent.parse("{\"payload\":{\"id\":{\"nested\":1}},\"id\":[1,2]}");

      assertThat(event.fileId()).isEmpty();
    }
  }

  @Nested
  class WhenJsonIsMalformed {
    @Test
    void givenMalformedJson_whenParsing_thenReturnsEmptyFields() {
      WebhookEvent event = WebhookEvent.parse("example");

      assertThat(event.trigger()).isEmpty();
      assertThat(event.fileId()).isEmpty();
    }
  }
}

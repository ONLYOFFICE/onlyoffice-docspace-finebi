package com.asc.fr.docspace.adapters.input.web.imports.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.asc.fr.docspace.adapters.input.web.imports.WebhookSignatureVerifierService;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class WebhookSignatureVerifierServiceTest {
  private static final String SECRET = "Abc12345Secret";

  private final WebhookSignatureVerifierService verifier = new WebhookSignatureVerifierService();

  private static String hmacHex(byte[] body, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    StringBuilder hex = new StringBuilder();

    for (byte b : mac.doFinal(body)) hex.append(String.format("%02X", b));

    return hex.toString();
  }

  static Stream<Arguments> validSignatureCases() throws Exception {
    byte[] jsonBody = "{\"event\":\"file.updated\"}".getBytes(StandardCharsets.UTF_8);
    byte[] plainBody = "payload".getBytes(StandardCharsets.UTF_8);
    return Stream.of(
        Arguments.of(jsonBody, "sha256=" + hmacHex(jsonBody, SECRET)),
        Arguments.of(plainBody, hmacHex(plainBody, SECRET)),
        Arguments.of(plainBody, hmacHex(plainBody, SECRET).toLowerCase()));
  }

  @Nested
  class WhenSignatureIsValid {
    @ParameterizedTest
    @MethodSource(
        "com.asc.fr.docspace.adapters.input.web.imports.handler.WebhookSignatureVerifierServiceTest#validSignatureCases")
    void givenCorrectHmac_whenVerifying_thenReturnsTrue(byte[] body, String signature) {
      assertThat(verifier.verify(body, SECRET, signature)).isTrue();
    }
  }

  @Nested
  class WhenSignatureIsInvalid {
    @Test
    void givenTamperedBody_whenVerifying_thenReturnsFalse() throws Exception {
      byte[] original = "payload".getBytes(StandardCharsets.UTF_8);
      String signature = "sha256=" + hmacHex(original, SECRET);
      byte[] tampered = "payload!".getBytes(StandardCharsets.UTF_8);

      assertThat(verifier.verify(tampered, SECRET, signature)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"sha256=zz-not-hex", "sha256=abc"})
    void givenInvalidHeader_whenVerifying_thenReturnsFalse(String header) {
      byte[] body = "payload".getBytes(StandardCharsets.UTF_8);

      assertThat(verifier.verify(body, SECRET, header)).isFalse();
    }
  }
}

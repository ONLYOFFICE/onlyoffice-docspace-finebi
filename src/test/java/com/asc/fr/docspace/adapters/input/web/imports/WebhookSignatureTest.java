package com.asc.fr.docspace.adapters.input.web.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.adapters.input.web.imports.transfer.WebhookSignature;
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

class WebhookSignatureTest {
  private static final String SECRET = "Abc12345Secret";

  private static String hmacHex(byte[] body, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    StringBuilder hex = new StringBuilder();

    for (byte b : mac.doFinal(body)) hex.append(String.format("%02X", b));

    return hex.toString();
  }

  static Stream<Arguments> validSignatureCases() {
    try {
      byte[] jsonBody = "{\"event\":\"file.updated\"}".getBytes(StandardCharsets.UTF_8);
      byte[] plainBody = "payload".getBytes(StandardCharsets.UTF_8);
      return Stream.of(
          Arguments.of(jsonBody, "sha256=" + hmacHex(jsonBody, SECRET)),
          Arguments.of(plainBody, hmacHex(plainBody, SECRET)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nested
  class WhenSignatureIsValid {
    @ParameterizedTest
    @MethodSource(
        "com.asc.fr.docspace.adapters.input.web.imports.WebhookSignatureTest#validSignatureCases")
    void givenCorrectHmac_whenVerifying_thenReturnsTrue(byte[] body, String signature)
        throws Exception {
      assertTrue(WebhookSignature.verify(body, SECRET, signature));
    }
  }

  @Nested
  class WhenSignatureIsInvalid {
    @Test
    void givenTamperedBody_whenVerifying_thenReturnsFalse() throws Exception {
      byte[] original = "payload".getBytes(StandardCharsets.UTF_8);
      String signature = "sha256=" + hmacHex(original, SECRET);
      byte[] tampered = "payload!".getBytes(StandardCharsets.UTF_8);
      assertFalse(WebhookSignature.verify(tampered, SECRET, signature));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"sha256=zz-not-hex"})
    void givenInvalidHeader_whenVerifying_thenReturnsFalse(String header) {
      byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
      assertFalse(WebhookSignature.verify(body, SECRET, header));
    }
  }
}

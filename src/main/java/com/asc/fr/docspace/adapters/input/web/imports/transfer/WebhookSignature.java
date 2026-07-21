package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies DocSpace's webhook signature header */
public final class WebhookSignature {
  private WebhookSignature() {}

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2)
      out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);

    return out;
  }

  public static boolean verify(byte[] body, String secret, String signatureHeader) {
    if (signatureHeader == null || signatureHeader.isEmpty()) return false;

    try {
      String hex = signatureHeader.trim();
      if (hex.startsWith("sha256=")) hex = hex.substring(7);

      byte[] received = hexToBytes(hex);
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

      byte[] expected = mac.doFinal(body);
      if (expected.length != received.length) return false;

      int diff = 0;
      for (int i = 0; i < expected.length; i++) diff |= expected[i] ^ received[i];

      return diff == 0;
    } catch (Exception e) {
      return false;
    }
  }
}

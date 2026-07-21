package com.asc.fr.docspace.adapters.input.web.imports;

import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies DocSpace's {@code x-docspace-signature-256} webhook header: an HMAC-SHA256 of the raw
 * request body, hex-encoded, optionally prefixed with {@code sha256=}.
 */
public class WebhookSignatureVerifierService {
  public boolean verify(byte[] body, String secret, String signatureHeader) {
    if (signatureHeader == null || signatureHeader.isEmpty()) return false;

    try {
      String hex = signatureHeader.trim();
      if (hex.startsWith("sha256=")) hex = hex.substring(7);

      byte[] received = BaseEncoding.base16().decode(hex.toUpperCase(Locale.ROOT));
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return MessageDigest.isEqual(mac.doFinal(body), received);
    } catch (Exception e) {
      return false;
    }
  }
}

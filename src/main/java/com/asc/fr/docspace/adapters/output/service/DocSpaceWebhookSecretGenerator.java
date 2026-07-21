package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.port.output.docspace.DocSpaceSecretGenerator;
import java.security.SecureRandom;

/**
 * Generates a 24-character alphanumeric secret that satisfies DocSpace's default password policy
 * (8-30 chars, latin letters + digits, no spaces). Guarantees at least one uppercase letter, one
 * lowercase letter, one digit.
 */
public final class DocSpaceWebhookSecretGenerator implements DocSpaceSecretGenerator {
  private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
  private static final String DIGITS = "23456789";
  private static final String ALL = UPPER + LOWER + DIGITS;

  private final SecureRandom rng = new SecureRandom();

  @Override
  public String generate() {
    char[] chars = new char[24];
    chars[0] = UPPER.charAt(rng.nextInt(UPPER.length()));
    chars[1] = LOWER.charAt(rng.nextInt(LOWER.length()));
    chars[2] = DIGITS.charAt(rng.nextInt(DIGITS.length()));

    for (int i = 3; i < chars.length; i++) chars[i] = ALL.charAt(rng.nextInt(ALL.length()));

    for (int i = chars.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      char tmp = chars[i];
      chars[i] = chars[j];
      chars[j] = tmp;
    }

    return new String(chars);
  }
}

package com.asc.fr.docspace.domain.docspace;

import com.asc.fr.docspace.domain.DomainValidator;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import lombok.Getter;

@Getter
public final class DocSpaceAccountCredentials {
  private static final DocSpaceAccountCredentials NO_CREDENTIALS = new DocSpaceAccountCredentials();

  private final String email;
  private final String userId;
  private final String hash;

  private static boolean isEmail(String value) {
    int at = value.indexOf('@');
    if (at <= 0 || at != value.lastIndexOf('@') || at == value.length() - 1) return false;
    String domain = value.substring(at + 1);
    return domain.indexOf('.') > 0 && !domain.startsWith(".") && !domain.endsWith(".");
  }

  private static String requireEmail(String value) {
    String email =
        DomainValidator.requirePresent(
            value,
            () ->
                new InvalidCredentialsException(
                    "Email must not be blank", "client.error.credentials.emailBlank"));

    if (!isEmail(email))
      throw new InvalidCredentialsException(
          "Email must be a valid address", "client.error.credentials.emailInvalid");

    return email;
  }

  private DocSpaceAccountCredentials() {
    this.email = "";
    this.userId = "";
    this.hash = "";
  }

  public DocSpaceAccountCredentials(String email, String userId, String hash) {
    this.email = requireEmail(email);
    this.userId =
        DomainValidator.requirePresent(
            userId,
            () ->
                new InvalidCredentialsException(
                    "User id must not be blank", "client.error.credentials.userIdBlank"));
    this.hash =
        DomainValidator.requirePresent(
            hash,
            () ->
                new InvalidCredentialsException(
                    "Hash must not be blank", "client.error.credentials.hashBlank"));
  }

  public static DocSpaceAccountCredentials empty() {
    return NO_CREDENTIALS;
  }

  public boolean isComplete() {
    return this != NO_CREDENTIALS;
  }
}

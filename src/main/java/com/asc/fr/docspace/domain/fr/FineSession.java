package com.asc.fr.docspace.domain.fr;

import com.asc.fr.docspace.domain.DomainValidator;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.exception.InvalidCookieException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class FineSession {
  private static final String AUTH_COOKIE = "fine_auth_token=";

  private final URL baseUrl;
  private final String cookie;

  public FineSession(String baseUrl, String cookie) {
    this.baseUrl = new URL(baseUrl);
    this.cookie =
        DomainValidator.requirePresent(
            cookie, () -> new InvalidCookieException("Cookie must not be blank"));
  }

  public String getAuthToken() {
    for (String part : cookie.split(";")) {
      String trimmed = part.trim();
      if (trimmed.startsWith(AUTH_COOKIE)) return trimmed.substring(AUTH_COOKIE.length()).trim();
    }

    return "";
  }
}

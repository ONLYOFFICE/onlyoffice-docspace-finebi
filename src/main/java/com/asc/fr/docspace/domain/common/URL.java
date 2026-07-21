package com.asc.fr.docspace.domain.common;

import com.asc.fr.docspace.domain.exception.InvalidUrlException;
import java.net.URI;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class URL {
  public static final URL EMPTY = new URL();
  private final String value;

  private URL() {
    this.value = "";
  }

  public URL(String url) {
    if (url == null || url.trim().isEmpty())
      throw new InvalidUrlException("URL cannot be null or empty");

    try {
      URI uri = new URI(url.trim());
      String scheme = uri.getScheme();

      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
        throw new InvalidUrlException("URL must use HTTP or HTTPS scheme");

      if (uri.getHost() == null) throw new InvalidUrlException("URL must contain a valid host");

      String normalized = uri.normalize().toString();
      if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);

      this.value = normalized;
    } catch (Exception e) {
      throw new InvalidUrlException("Invalid URL format: " + url);
    }
  }

  public static boolean isValid(String url) {
    try {
      new URL(url);
      return true;
    } catch (InvalidUrlException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return value;
  }
}

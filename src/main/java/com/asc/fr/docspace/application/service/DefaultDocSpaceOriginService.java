package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceOriginService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceCspService;
import com.asc.fr.docspace.domain.common.URL;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceOriginService implements DocSpaceOriginService {
  private final DocSpaceCspService cspService;

  private static String normalizeHost(String value) {
    if (value == null) return "";

    String trimmed = value.trim().toLowerCase();
    if (trimmed.isEmpty()) return "";

    try {
      URI uri = trimmed.contains("://") ? new URI(trimmed) : new URI("https://" + trimmed);
      String host = uri.getHost();
      if (host == null || host.isEmpty()) return "";

      String path = uri.getPath();
      if (path != null && !path.isEmpty() && !"/".equals(path)) return host + path;

      return host;
    } catch (URISyntaxException e) {
      return trimmed;
    }
  }

  private static boolean isOriginAllowed(List<String> domains, String origin) {
    if (origin == null || origin.trim().isEmpty()) return false;

    String host = normalizeHost(origin);
    if (host.isEmpty()) return false;

    for (String domain : domains) if (host.equalsIgnoreCase(normalizeHost(domain))) return true;

    return false;
  }

  @Override
  public OriginCheck checkOrigin(URL docSpaceUrl, String origin) {
    try {
      List<String> domains = cspService.allowedDomains(docSpaceUrl);
      if (domains.isEmpty()) return OriginCheck.UNKNOWN;
      return isOriginAllowed(domains, origin) ? OriginCheck.ALLOWED : OriginCheck.BLOCKED;
    } catch (IOException e) {
      return OriginCheck.UNKNOWN;
    }
  }
}

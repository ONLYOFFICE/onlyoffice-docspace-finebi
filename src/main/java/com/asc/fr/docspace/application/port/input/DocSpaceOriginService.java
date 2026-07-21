package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.domain.common.URL;

public interface DocSpaceOriginService {
  enum OriginCheck {
    ALLOWED,
    BLOCKED,
    UNKNOWN
  }

  OriginCheck checkOrigin(URL docSpaceUrl, String origin);
}

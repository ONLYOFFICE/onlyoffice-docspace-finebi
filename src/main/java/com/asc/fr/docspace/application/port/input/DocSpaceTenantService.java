package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;

public interface DocSpaceTenantService {
  boolean isConfigured();

  String docSpaceUrl();

  DocSpaceAccountCredentials adminCredentials();
}

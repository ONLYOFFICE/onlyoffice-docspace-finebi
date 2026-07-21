package com.asc.fr.docspace.application.port.output.docspace;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceAuthenticator {
  String authenticate(URL baseUrl, DocSpaceAccountCredentials credentials) throws IOException;

  String bearer(URL baseUrl, DocSpaceAccountCredentials credentials) throws IOException;
}

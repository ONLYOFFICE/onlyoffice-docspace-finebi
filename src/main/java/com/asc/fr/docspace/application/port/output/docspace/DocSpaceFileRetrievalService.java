package com.asc.fr.docspace.application.port.output.docspace;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceFileRetrievalService {
  boolean fileExists(URL docSpaceUrl, String fileId, DocSpaceAccountCredentials credentials)
      throws IOException;
}

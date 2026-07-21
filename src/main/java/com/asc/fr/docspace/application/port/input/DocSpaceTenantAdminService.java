package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceTenantAdminService {
  void save(URL docSpaceUrl, DocSpaceAccountCredentials admin) throws IOException;

  void reset() throws IOException;
}

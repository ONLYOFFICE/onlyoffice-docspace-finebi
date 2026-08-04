package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceTenantAdminService {
  void save(URL docSpaceUrl, DocSpaceAccountCredentials admin) throws IOException;

  /** Switches to a different DocSpace connection without touching existing sync links. */
  void changeTenant() throws IOException;

  /** Wipes the tenant connection and every sync link. */
  void reset() throws IOException;
}

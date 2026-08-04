package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceTenantAdminService {
  void save(URL docSpaceUrl, DocSpaceAccountCredentials admin) throws IOException;

  /** Switches to a different DocSpace connection without touching existing sync links. */
  void changeTenant() throws IOException;

  /**
   * Activates a previously saved DocSpace connection (restores its admin credentials and webhook
   * secret as the current tenant). Preserves the outgoing active tenant in saved history.
   */
  void selectTenant(URL docSpaceUrl) throws IOException;

  /**
   * Removes one DocSpace connection: its saved credentials/secret and every sync link for that
   * tenant URL. If it is the currently active tenant, the active row is cleared too.
   */
  void removeTenant(URL docSpaceUrl) throws IOException;

  /** Wipes the tenant connection and every sync link. */
  void reset() throws IOException;
}

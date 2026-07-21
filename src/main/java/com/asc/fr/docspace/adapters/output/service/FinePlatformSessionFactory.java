package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.exception.SessionGenerationException;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.fr.decision.webservice.v10.login.LoginService;
import com.fr.decision.webservice.v10.user.UserService;
import com.fr.tenant.context.TenantContext;
import java.util.List;

/**
 * Mints FineBI admin sessions via the in-JVM. Resolves the admin username and tenant from FineBI
 * instead of hardcoding them. validity = -1 means "no expiry" in FineBI's TokenIDCheckPolicy.
 */
public final class FinePlatformSessionFactory implements FineSessionFactory {
  private static String resolveAdminUsername() throws Exception {
    List<String> admins = UserService.getInstance().getAdminUserNameList();
    if (admins == null || admins.isEmpty()) return null;

    return admins.get(0);
  }

  @Override
  public FineSession generateSession(String baseUrl) {
    try {
      String username = resolveAdminUsername();
      if (username == null) return new FineSession(baseUrl, "");

      String tenantId = TenantContext.getCurrentOrDefault().getId();
      String token = LoginService.getInstance().generateAndStoreToken(username, tenantId, -1);
      if (token != null && !token.isEmpty())
        return new FineSession(baseUrl, "fine_auth_token=" + token);
    } catch (Exception e) {
      throw new SessionGenerationException(e.getMessage());
    }

    return new FineSession(baseUrl, "");
  }
}

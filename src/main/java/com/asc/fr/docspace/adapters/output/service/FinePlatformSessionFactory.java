package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.exception.SessionGenerationException;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.fr.decision.authority.data.User;
import com.fr.decision.service.DecisionServiceManager;
import com.fr.decision.service.authority.DecisionUserServiceProvider;
import com.fr.decision.service.login.DecisionLoginServiceProvider;
import com.fr.decision.webservice.bean.authentication.LoginClientBean;
import com.fr.decision.webservice.v10.login.validity.LoginValidityType;
import com.fr.decision.webservice.v10.user.UserService;
import com.fr.tenant.context.TenantContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mints FineBI admin sessions via the in-JVM APIs. Tokens live 15 minutes in FineBI; an in-memory
 * cache reuses them for 10 minutes so background work does not remint on every call.
 */
public final class FinePlatformSessionFactory implements FineSessionFactory {
  private static final long TOKEN_TTL_MILLIS = TimeUnit.MINUTES.toMillis(15);
  private static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

  private final Cache<String, String> tokens =
      CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMillis(CACHE_TTL_MILLIS)).build();

  private static String resolveAdminUsername() throws Exception {
    List<String> admins = UserService.getInstance().getAdminUserNameList();
    if (admins == null || admins.isEmpty()) return null;

    return admins.get(0);
  }

  private static String mintToken(String username, String tenantId) throws Exception {
    DecisionLoginServiceProvider loginService =
        DecisionServiceManager.getInstance().getService(DecisionLoginServiceProvider.class);
    DecisionUserServiceProvider userService =
        DecisionServiceManager.getInstance().getService(DecisionUserServiceProvider.class);

    User user = userService.getUserByUserName(username);
    String token =
        loginService.generateToken(
            user.getUserName(), user.getDisplayName(), tenantId, TOKEN_TTL_MILLIS);

    LoginClientBean status = new LoginClientBean();
    status.setUsername(user.getUserName());
    status.setToken(token);
    status.setUserId(user.getId());
    status.setValidity(LoginValidityType.UNREMEMBERED_PASSWORD.getValidity());
    loginService.addLoginStatus(token, status, TOKEN_TTL_MILLIS);

    return token;
  }

  private String tokenFor(String username, String tenantId) throws Exception {
    String key = tenantId + '|' + username;
    String cached = tokens.getIfPresent(key);
    if (cached != null && !cached.isEmpty()) return cached;

    synchronized (tokens) {
      cached = tokens.getIfPresent(key);
      if (cached != null && !cached.isEmpty()) return cached;

      String token =
          TenantContext.doIsolatedResultWork(() -> mintToken(username, tenantId), tenantId);

      if (token != null && !token.isEmpty()) tokens.put(key, token);

      return token;
    }
  }

  @Override
  public FineSession generateSession(String baseUrl) {
    try {
      String username = resolveAdminUsername();
      if (username == null) return new FineSession(baseUrl, "");

      String tenantId = TenantContext.getCurrentOrDefault().getId();
      String token = tokenFor(username, tenantId);
      if (token != null && !token.isEmpty())
        return new FineSession(baseUrl, "fine_auth_token=" + token);
    } catch (Exception e) {
      throw new SessionGenerationException(e.getMessage());
    }

    return new FineSession(baseUrl, "");
  }
}

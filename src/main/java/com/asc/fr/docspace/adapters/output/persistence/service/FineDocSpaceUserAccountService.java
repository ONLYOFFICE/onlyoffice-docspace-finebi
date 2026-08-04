package com.asc.fr.docspace.adapters.output.persistence.service;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceAccountDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceKeysetPaginationDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceAccountEntity;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.domain.DocSpaceUserAccountService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class FineDocSpaceUserAccountService implements DocSpaceUserAccountService {
  private static final long CACHE_TTL_MILLIS = 200;
  private static final long CACHE_MAX_USERS = 1_000;

  private final FineEncryptionService encryption;
  private final IUnitOfWork uow;

  private final Cache<String, CachedAccount> cache =
      CacheBuilder.newBuilder()
          .maximumSize(CACHE_MAX_USERS)
          .expireAfterWrite(Duration.ofMillis(CACHE_TTL_MILLIS))
          .build();

  @Value
  private static final class CachedAccount {
    DocSpaceAccountCredentials credentials;
    String tenantUrl;
  }

  private CachedAccount load(String id) {
    CachedAccount cached = cache.getIfPresent(id);
    if (cached != null) return cached;

    CachedAccount loaded =
        uow.query(ctx -> ctx.getDAO(DocSpaceAccountDAO.class).getById(id))
            .map(
                entity -> {
                  try {
                    DocSpaceAccountCredentials credentials =
                        new DocSpaceAccountCredentials(
                            encryption.decrypt(entity.getEmail()),
                            entity.getDocspaceUserId(),
                            encryption.decrypt(entity.getPasswordHash()));
                    String tenantUrl = entity.getTenantUrl();
                    return new CachedAccount(credentials, tenantUrl == null ? "" : tenantUrl);
                  } catch (InvalidCredentialsException e) {
                    return new CachedAccount(DocSpaceAccountCredentials.empty(), "");
                  }
                })
            .orElseGet(() -> new CachedAccount(DocSpaceAccountCredentials.empty(), ""));

    cache.put(id, loaded);
    return loaded;
  }

  @Override
  public DocSpaceAccountCredentials credentials(String username) {
    String id = username == null ? "" : username.trim();
    if (id.isEmpty()) return DocSpaceAccountCredentials.empty();
    return load(id).getCredentials();
  }

  @Override
  public String signedInTenantUrl(String username) {
    String id = username == null ? "" : username.trim();
    if (id.isEmpty()) return "";
    CachedAccount account = load(id);
    return account.getCredentials().isComplete() ? account.getTenantUrl() : "";
  }

  @Override
  public void saveCredentials(
      String username, DocSpaceAccountCredentials credentials, String tenantUrl)
      throws IOException {
    String id = username == null ? "" : username.trim();
    if (id.isEmpty()) throw new IOException("FineBI user is not available in this request");

    String portal = tenantUrl == null ? "" : tenantUrl;

    uow.write(
        ctx -> {
          DocSpaceAccountDAO dao = ctx.getDAO(DocSpaceAccountDAO.class);
          DocSpaceAccountEntity entity = dao.getById(id);
          if (entity == null) {
            entity = new DocSpaceAccountEntity();
            entity.setId(id);
          }

          entity.setEmail(encryption.encrypt(credentials.getEmail()));
          entity.setDocspaceUserId(credentials.getUserId());
          entity.setPasswordHash(encryption.encrypt(credentials.getHash()));
          entity.setTenantUrl(portal);
          dao.addOrUpdate(entity);
          return null;
        });

    cache.invalidate(id);
  }

  @Override
  public void clear(String username) throws IOException {
    String id = username == null ? "" : username.trim();
    if (id.isEmpty()) {
      return;
    }

    uow.write(
        ctx -> {
          DocSpaceAccountDAO dao = ctx.getDAO(DocSpaceAccountDAO.class);
          if (dao.getById(id) != null) {
            dao.remove(id);
          }
        });

    cache.invalidate(id);
  }

  @Override
  public void clearAll() throws IOException {
    DocSpaceKeysetPaginationDAO.deleteAll(uow, DocSpaceAccountDAO.class);
    cache.invalidateAll();
  }
}

package com.asc.fr.docspace.adapters.output.persistence.service;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceTenantDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceTenantEntity;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class FineDocSpaceTenantService implements DocSpaceTenantService {
  private static final long CACHE_TTL_MILLIS = 500;

  private final FineEncryptionService encryption;
  private final IUnitOfWork uow;

  private final Cache<String, DocSpaceTenantConfiguration> cache =
      CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMillis(CACHE_TTL_MILLIS)).build();

  @Override
  public DocSpaceTenantConfiguration load() {
    DocSpaceTenantConfiguration cached = cache.getIfPresent(DocSpaceTenantEntity.SINGLETON_ID);
    if (cached != null) return cached;

    DocSpaceTenantConfiguration loaded = loadFromStore();
    cache.put(DocSpaceTenantEntity.SINGLETON_ID, loaded);
    return loaded;
  }

  private DocSpaceTenantConfiguration loadFromStore() {
    return uow.query(
            ctx -> ctx.getDAO(DocSpaceTenantDAO.class).getById(DocSpaceTenantEntity.SINGLETON_ID))
        .filter(entity -> entity.getUrl() != null && !entity.getUrl().isEmpty())
        .map(
            entity -> {
              DocSpaceAccountCredentials admin;
              try {
                admin =
                    new DocSpaceAccountCredentials(
                        encryption.decrypt(entity.getAdminEmail()),
                        entity.getAdminUserId(),
                        encryption.decrypt(entity.getAdminHash()));
              } catch (InvalidCredentialsException e) {
                admin = DocSpaceAccountCredentials.empty();
              }
              return new DocSpaceTenantConfiguration(entity.getUrl(), admin);
            })
        .orElseGet(DocSpaceTenantConfiguration::empty);
  }

  @Override
  public void save(DocSpaceTenantConfiguration config) throws IOException {
    uow.write(
        ctx -> {
          DocSpaceTenantDAO dao = ctx.getDAO(DocSpaceTenantDAO.class);
          DocSpaceTenantEntity entity = dao.getById(DocSpaceTenantEntity.SINGLETON_ID);
          if (entity == null) {
            entity = new DocSpaceTenantEntity();
            entity.setId(DocSpaceTenantEntity.SINGLETON_ID);
          }

          entity.setUrl(config.getUrl().getValue());

          if (config.getAdmin().isComplete()) {
            entity.setAdminEmail(encryption.encrypt(config.getAdmin().getEmail()));
            entity.setAdminUserId(config.getAdmin().getUserId());
            entity.setAdminHash(encryption.encrypt(config.getAdmin().getHash()));
          } else {
            // Soft reset / incomplete admin: drop credentials so isConfigured()
            // becomes false while the DocSpace URL can remain for setup + SDK preload.
            entity.setAdminEmail("");
            entity.setAdminUserId("");
            entity.setAdminHash("");
          }

          dao.addOrUpdate(entity);
          return null;
        });

    cache.invalidateAll();
  }

  @Override
  public void clear() throws IOException {
    uow.write(
        ctx -> {
          DocSpaceTenantDAO dao = ctx.getDAO(DocSpaceTenantDAO.class);
          if (dao.getById(DocSpaceTenantEntity.SINGLETON_ID) != null) {
            dao.remove(DocSpaceTenantEntity.SINGLETON_ID);
          }
        });

    cache.invalidateAll();
  }
}

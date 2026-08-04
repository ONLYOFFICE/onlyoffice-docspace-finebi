package com.asc.fr.docspace.adapters.output.persistence.service;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceKeysetPaginationDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSavedTenantDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSavedTenantEntity;
import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.fr.stable.query.QueryFactory;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class FineDocSpaceSavedTenantService implements DocSpaceSavedTenantService {
  private static final int MAX_SAVED_TENANTS = 2;

  private final FineEncryptionService encryption;
  private final IUnitOfWork uow;

  private static String keyFor(String url) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) hex.append(String.format("%02x", b));
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void upsert(DocSpaceTenantConfiguration config, String webhookSecret) throws IOException {
    if (config == null) return;

    String url = config.getUrl().getValue();
    if (url.isEmpty()) return;
    if (!config.getAdmin().isComplete()) return;

    String key = keyFor(url);
    String secret = webhookSecret == null ? "" : webhookSecret;

    uow.write(
        ctx -> {
          DocSpaceSavedTenantDAO dao = ctx.getDAO(DocSpaceSavedTenantDAO.class);
          DocSpaceSavedTenantEntity entity = dao.getById(key);
          if (entity == null) {
            if (dao.find(QueryFactory.create()).size() >= MAX_SAVED_TENANTS)
              throw new TenantLimitExceededException(
                  "Remove a saved DocSpace connection before registering a different tenant. Maximum number of saved tenants reached.");

            entity = new DocSpaceSavedTenantEntity();
            entity.setId(key);
            entity.setUrl(url);
          }

          entity.setAdminEmail(encryption.encrypt(config.getAdmin().getEmail()));
          entity.setAdminUserId(config.getAdmin().getUserId());
          entity.setAdminHash(encryption.encrypt(config.getAdmin().getHash()));
          entity.setWebhookSecret(secret.isEmpty() ? "" : encryption.encrypt(secret));
          entity.setLastUsedAt(System.currentTimeMillis());
          dao.addOrUpdate(entity);
          return null;
        });
  }

  @Override
  public Optional<DocSpaceTenantConfiguration> find(String url) {
    if (url == null || url.isEmpty()) return Optional.empty();

    return uow.query(ctx -> ctx.getDAO(DocSpaceSavedTenantDAO.class).getById(keyFor(url)))
        .map(
            entity -> {
              try {
                DocSpaceAccountCredentials admin =
                    new DocSpaceAccountCredentials(
                        encryption.decrypt(entity.getAdminEmail()),
                        entity.getAdminUserId(),
                        encryption.decrypt(entity.getAdminHash()));
                return new DocSpaceTenantConfiguration(entity.getUrl(), admin);
              } catch (InvalidCredentialsException e) {
                return null;
              }
            });
  }

  @Override
  public boolean hasCapacityFor(String url) {
    if (url == null || url.isEmpty()) return true;

    String key = keyFor(url);
    return uow.query(
            ctx -> {
              DocSpaceSavedTenantDAO dao = ctx.getDAO(DocSpaceSavedTenantDAO.class);
              if (dao.getById(key) != null) return true;
              return hasRoomForOneMore(dao);
            })
        .orElse(true);
  }

  private static boolean hasRoomForOneMore(DocSpaceSavedTenantDAO dao) throws Exception {
    return dao.find(QueryFactory.create()).size() < MAX_SAVED_TENANTS;
  }

  @Override
  public List<DocSpaceSavedTenantConnection> listConnections() {
    List<DocSpaceSavedTenantEntity> rows =
        uow.query(ctx -> ctx.getDAO(DocSpaceSavedTenantDAO.class).find(QueryFactory.create()))
            .orElse(Collections.emptyList());

    List<DocSpaceSavedTenantConnection> connections = new ArrayList<>(rows.size());
    for (DocSpaceSavedTenantEntity entity : rows) {
      if (entity.getUrl() == null || entity.getUrl().isEmpty()) continue;

      try {
        DocSpaceAccountCredentials admin =
            new DocSpaceAccountCredentials(
                encryption.decrypt(entity.getAdminEmail()),
                entity.getAdminUserId(),
                encryption.decrypt(entity.getAdminHash()));
        String storedSecret = entity.getWebhookSecret();
        String secret =
            storedSecret == null || storedSecret.isEmpty() ? "" : encryption.decrypt(storedSecret);
        connections.add(
            new DocSpaceSavedTenantConnection(
                new DocSpaceTenantConfiguration(entity.getUrl(), admin), secret));
      } catch (InvalidCredentialsException e) {
        // Corrupted/undecryptable row — skip it rather than fail the whole listing.
      }
    }
    return connections;
  }

  @Override
  public boolean hasAny() {
    return uow.query(
                ctx ->
                    ctx.getDAO(DocSpaceSavedTenantDAO.class).find(QueryFactory.create().count(1)))
            .orElse(Collections.emptyList())
            .size()
        > 0;
  }

  @Override
  public boolean canAddNew() {
    return uow.query(ctx -> hasRoomForOneMore(ctx.getDAO(DocSpaceSavedTenantDAO.class)))
        .orElse(true);
  }

  @Override
  public void remove(String url) throws IOException {
    if (url == null || url.isEmpty()) return;
    String key = keyFor(url);
    uow.write(
        ctx -> {
          DocSpaceSavedTenantDAO dao = ctx.getDAO(DocSpaceSavedTenantDAO.class);
          if (dao.getById(key) != null) dao.remove(key);
          return null;
        });
  }

  @Override
  public void clearAll() throws IOException {
    DocSpaceKeysetPaginationDAO.deleteAll(uow, DocSpaceSavedTenantDAO.class);
  }
}

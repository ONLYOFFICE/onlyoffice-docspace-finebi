package com.asc.fr.docspace.adapters.output.persistence.service;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSynchronizationEntryDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSynchronizationSettingsDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationEntryEntity;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationSettingsEntity;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceSecretGenerator;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.domain.SynchronizationService;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.fr.stable.query.QueryFactory;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class FineSynchronizationService implements SynchronizationService {
  private final FineEncryptionService encryption;
  private final DocSpaceSecretGenerator docSpaceSecretGenerator;
  private final IUnitOfWork uow;

  @Override
  public void put(String fileId, FileSynchronizationRecord entry) throws IOException {
    if (fileId == null || fileId.isEmpty() || entry == null) return;
    uow.write(
        ctx -> {
          DocSpaceSynchronizationEntryDAO dao = ctx.getDAO(DocSpaceSynchronizationEntryDAO.class);
          DocSpaceSynchronizationEntryEntity entity = dao.getById(fileId);
          if (entity == null) {
            entity = new DocSpaceSynchronizationEntryEntity();
            entity.setId(fileId);
          }

          entity.setTableName(entry.getTableName());
          entity.setFolderId(entry.getFolderId());
          entity.setTableId(entry.getTableId());
          dao.addOrUpdate(entity);
          return null;
        });
  }

  @Override
  public FileSynchronizationRecord find(String fileId) {
    if (fileId == null || fileId.isEmpty()) return null;
    return uow.query(ctx -> ctx.getDAO(DocSpaceSynchronizationEntryDAO.class).getById(fileId))
        .map(
            entity ->
                new FileSynchronizationRecord(
                    entity.getTableName(), entity.getFolderId(), entity.getTableId()))
        .orElse(null);
  }

  @Override
  public void remove(String fileId) throws IOException {
    if (fileId == null || fileId.isEmpty()) return;

    uow.write(
        ctx -> {
          DocSpaceSynchronizationEntryDAO dao = ctx.getDAO(DocSpaceSynchronizationEntryDAO.class);
          if (dao.getById(fileId) != null) dao.remove(fileId);
        });
  }

  @Override
  public Map<String, FileSynchronizationRecord> entries() {
    List<DocSpaceSynchronizationEntryEntity> rows =
        uow.query(
                ctx ->
                    ctx.getDAO(DocSpaceSynchronizationEntryDAO.class).find(QueryFactory.create()))
            .orElse(Collections.emptyList());
    Map<String, FileSynchronizationRecord> response = new LinkedHashMap<>();
    for (DocSpaceSynchronizationEntryEntity entity : rows) {
      if (entity.getId() == null || entity.getId().isEmpty()) continue;
      response.put(
          entity.getId(),
          new FileSynchronizationRecord(
              entity.getTableName(), entity.getFolderId(), entity.getTableId()));
    }

    return response;
  }

  @Override
  public void storeCallbackUrl(String url) throws IOException {
    if (url == null || url.isEmpty()) return;
    uow.write(
        ctx -> {
          DocSpaceSynchronizationSettingsDAO dao =
              ctx.getDAO(DocSpaceSynchronizationSettingsDAO.class);
          DocSpaceSynchronizationSettingsEntity entity =
              dao.getById(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID);
          if (entity == null) {
            entity = new DocSpaceSynchronizationSettingsEntity();
            entity.setId(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID);
          }

          entity.setCallbackUrl(url);
          dao.addOrUpdate(entity);
          return null;
        });
  }

  @Override
  public String loadCallbackUrl() {
    return uow.query(
            ctx ->
                ctx.getDAO(DocSpaceSynchronizationSettingsDAO.class)
                    .getById(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID))
        .map(DocSpaceSynchronizationSettingsEntity::getCallbackUrl)
        .orElse("");
  }

  @Override
  public String loadSecret() {
    String stored =
        uow.query(
                ctx ->
                    ctx.getDAO(DocSpaceSynchronizationSettingsDAO.class)
                        .getById(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID))
            .map(DocSpaceSynchronizationSettingsEntity::getWebhookSecret)
            .orElse("");
    if (stored == null || stored.isEmpty()) return "";
    String secret = encryption.decrypt(stored);
    if (secret.length() > 30 || secret.equals(secret.toLowerCase())) return "";
    return secret;
  }

  @Override
  public String ensureSecret() throws IOException {
    return uow.write(
        ctx -> {
          DocSpaceSynchronizationSettingsDAO dao =
              ctx.getDAO(DocSpaceSynchronizationSettingsDAO.class);
          DocSpaceSynchronizationSettingsEntity entity =
              dao.getById(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID);
          if (entity == null) {
            entity = new DocSpaceSynchronizationSettingsEntity();
            entity.setId(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID);
          }

          String stored = entity.getWebhookSecret();
          String secret = (stored == null || stored.isEmpty()) ? "" : encryption.decrypt(stored);
          if (secret.isEmpty() || secret.length() > 30 || secret.equals(secret.toLowerCase())) {
            secret = docSpaceSecretGenerator.generate();
            entity.setWebhookSecret(encryption.encrypt(secret));
            dao.addOrUpdate(entity);
          }

          return secret;
        });
  }
}

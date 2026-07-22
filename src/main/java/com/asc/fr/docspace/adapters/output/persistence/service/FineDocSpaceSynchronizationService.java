package com.asc.fr.docspace.adapters.output.persistence.service;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceKeysetPaginationDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSynchronizationEntryDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSynchronizationSettingsDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationEntryEntity;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationSettingsEntity;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceSecretGenerator;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.fr.stable.query.QueryFactory;
import com.fr.stable.query.condition.QueryCondition;
import com.fr.stable.query.restriction.RestrictionFactory;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class FineDocSpaceSynchronizationService
    implements SynchronizationLinkRegistry, SynchronizationSettings {
  private static final int STALE_SCAN_BATCH = 100;

  private final FineEncryptionService encryption;
  private final DocSpaceSecretGenerator docSpaceSecretGenerator;
  private final IUnitOfWork uow;

  private static FileSynchronizationRecord toRecord(DocSpaceSynchronizationEntryEntity entity) {
    Long stamp = entity.getLastReconciledAt();
    return new FileSynchronizationRecord(
        entity.getFileId(),
        entity.getTableName(),
        entity.getFolderId(),
        entity.getId(),
        stamp == null ? 0L : stamp);
  }

  private static QueryCondition byFileId(String fileId) {
    return QueryFactory.create().addRestriction(RestrictionFactory.eq("fileId", fileId));
  }

  private List<FileSynchronizationRecord> page(QueryCondition condition) {
    List<DocSpaceSynchronizationEntryEntity> rows =
        uow.query(ctx -> ctx.getDAO(DocSpaceSynchronizationEntryDAO.class).find(condition))
            .orElse(Collections.emptyList());
    List<FileSynchronizationRecord> response = new ArrayList<>(rows.size());
    for (DocSpaceSynchronizationEntryEntity entity : rows) {
      if (entity.getId() == null || entity.getId().isEmpty()) continue;
      response.add(toRecord(entity));
    }

    return response;
  }

  @Override
  public void put(FileSynchronizationRecord entry) throws IOException {
    if (entry == null) return;

    String tableId = entry.getTableId();
    if (tableId == null || tableId.isEmpty()) return;

    uow.write(
        ctx -> {
          DocSpaceSynchronizationEntryDAO dao = ctx.getDAO(DocSpaceSynchronizationEntryDAO.class);
          DocSpaceSynchronizationEntryEntity entity = dao.getById(tableId);
          if (entity == null) {
            entity = new DocSpaceSynchronizationEntryEntity();
            entity.setId(tableId);
          }

          entity.setFileId(entry.getFileId());
          entity.setTableName(entry.getTableName());
          entity.setFolderId(entry.getFolderId());
          entity.setLastReconciledAt(System.currentTimeMillis());
          dao.addOrUpdate(entity);
          return null;
        });
  }

  @Override
  public List<FileSynchronizationRecord> findByFile(String fileId) {
    if (fileId == null || fileId.isEmpty()) return Collections.emptyList();
    return page(byFileId(fileId));
  }

  @Override
  public void remove(String tableId) throws IOException {
    if (tableId == null || tableId.isEmpty()) return;

    uow.write(
        ctx -> {
          DocSpaceSynchronizationEntryDAO dao = ctx.getDAO(DocSpaceSynchronizationEntryDAO.class);
          if (dao.getById(tableId) != null) dao.remove(tableId);
        });
  }

  @Override
  public void removeByFile(String fileId) throws IOException {
    if (fileId == null || fileId.isEmpty()) return;

    uow.write(
        ctx -> {
          DocSpaceSynchronizationEntryDAO dao = ctx.getDAO(DocSpaceSynchronizationEntryDAO.class);
          for (DocSpaceSynchronizationEntryEntity entity : dao.find(byFileId(fileId)))
            dao.remove(entity.getId());
        });
  }

  @Override
  public void removeAll() throws IOException {
    DocSpaceKeysetPaginationDAO.deleteAll(uow, DocSpaceSynchronizationEntryDAO.class);
  }

  @Override
  public List<FileSynchronizationRecord> staleLinks(
      long reconciledBefore, String afterTableId, int limit) {
    if (limit <= 0) return Collections.emptyList();

    List<FileSynchronizationRecord> stale = new ArrayList<>();
    String cursor = afterTableId == null ? "" : afterTableId;
    while (stale.size() < limit) {
      final String after = cursor;
      List<DocSpaceSynchronizationEntryEntity> rows =
          uow.query(
                  ctx ->
                      ctx.getDAO(DocSpaceSynchronizationEntryDAO.class)
                          .find(DocSpaceKeysetPaginationDAO.byId(after, STALE_SCAN_BATCH, null)))
              .orElse(Collections.emptyList());
      if (rows.isEmpty()) break;

      for (DocSpaceSynchronizationEntryEntity entity : rows) {
        if (entity.getId() == null || entity.getId().isEmpty()) continue;

        cursor = entity.getId();
        Long stamp = entity.getLastReconciledAt();
        if (stamp != null && stamp >= reconciledBefore) continue; // still fresh

        stale.add(toRecord(entity));
        if (stale.size() >= limit) break;
      }

      if (rows.size() < STALE_SCAN_BATCH) break;
    }

    return stale;
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
  public void storeDecisionBase(String decisionBase) throws IOException {
    if (decisionBase == null || decisionBase.isEmpty()) return;
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

          if (decisionBase.equals(entity.getDecisionBase())) return null;
          entity.setDecisionBase(decisionBase);
          dao.addOrUpdate(entity);
          return null;
        });
  }

  @Override
  public String loadDecisionBase() {
    return uow.query(
            ctx ->
                ctx.getDAO(DocSpaceSynchronizationSettingsDAO.class)
                    .getById(DocSpaceSynchronizationSettingsEntity.SINGLETON_ID))
        .map(DocSpaceSynchronizationSettingsEntity::getDecisionBase)
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
    if (stored.isEmpty()) return "";
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

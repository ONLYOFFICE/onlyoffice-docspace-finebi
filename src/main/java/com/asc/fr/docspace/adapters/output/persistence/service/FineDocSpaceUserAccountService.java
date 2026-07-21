package com.asc.fr.docspace.adapters.output.persistence.service;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceAccountDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceAccountEntity;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.domain.DocSpaceUserAccountService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.fr.stable.query.QueryFactory;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class FineDocSpaceUserAccountService implements DocSpaceUserAccountService {
  private final FineEncryptionService encryption;
  private final IUnitOfWork uow;

  @Override
  public DocSpaceAccountCredentials credentials(String username) {
    return uow.query(
            ctx ->
                ctx.getDAO(DocSpaceAccountDAO.class)
                    .getById(username == null ? "" : username.trim()))
        .map(
            entity -> {
              try {
                return new DocSpaceAccountCredentials(
                    encryption.decrypt(entity.getEmail()),
                    entity.getDocspaceUserId(),
                    encryption.decrypt(entity.getPasswordHash()));
              } catch (InvalidCredentialsException e) {
                return DocSpaceAccountCredentials.empty();
              }
            })
        .orElseGet(DocSpaceAccountCredentials::empty);
  }

  @Override
  public void saveCredentials(String username, DocSpaceAccountCredentials credentials)
      throws IOException {
    String id = username == null ? "" : username.trim();
    if (id.isEmpty()) throw new IOException("FineBI user is not available in this request");

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
          dao.addOrUpdate(entity);
          return null;
        });
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
  }

  @Override
  public void clearAll() throws IOException {
    uow.write(
        ctx -> {
          DocSpaceAccountDAO dao = ctx.getDAO(DocSpaceAccountDAO.class);
          for (DocSpaceAccountEntity row : dao.find(QueryFactory.create())) {
            dao.remove(row.getId());
          }
        });
  }
}

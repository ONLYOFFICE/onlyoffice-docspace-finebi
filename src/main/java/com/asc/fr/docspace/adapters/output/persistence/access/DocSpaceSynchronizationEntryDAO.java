package com.asc.fr.docspace.adapters.output.persistence.access;

import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationEntryEntity;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.session.DAOSession;

public class DocSpaceSynchronizationEntryDAO extends BaseDAO<DocSpaceSynchronizationEntryEntity> {
  public DocSpaceSynchronizationEntryDAO(DAOSession session) {
    super(session);
  }

  @Override
  protected Class<DocSpaceSynchronizationEntryEntity> getEntityClass() {
    return DocSpaceSynchronizationEntryEntity.class;
  }
}

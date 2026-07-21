package com.asc.fr.docspace.adapters.output.persistence.access;

import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceAccountEntity;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.session.DAOSession;

public class DocSpaceAccountDAO extends BaseDAO<DocSpaceAccountEntity> {
  public DocSpaceAccountDAO(DAOSession session) {
    super(session);
  }

  @Override
  protected Class<DocSpaceAccountEntity> getEntityClass() {
    return DocSpaceAccountEntity.class;
  }
}

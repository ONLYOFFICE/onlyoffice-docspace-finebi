package com.asc.fr.docspace.adapters.output.persistence.access;

import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceTenantEntity;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.session.DAOSession;

public class DocSpaceTenantDAO extends BaseDAO<DocSpaceTenantEntity> {
  public DocSpaceTenantDAO(DAOSession session) {
    super(session);
  }

  @Override
  protected Class<DocSpaceTenantEntity> getEntityClass() {
    return DocSpaceTenantEntity.class;
  }
}

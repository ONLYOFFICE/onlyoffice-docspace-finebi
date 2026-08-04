package com.asc.fr.docspace.adapters.output.persistence.access;

import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSavedTenantEntity;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.session.DAOSession;

public class DocSpaceSavedTenantDAO extends BaseDAO<DocSpaceSavedTenantEntity> {
  public DocSpaceSavedTenantDAO(DAOSession session) {
    super(session);
  }

  @Override
  protected Class<DocSpaceSavedTenantEntity> getEntityClass() {
    return DocSpaceSavedTenantEntity.class;
  }
}

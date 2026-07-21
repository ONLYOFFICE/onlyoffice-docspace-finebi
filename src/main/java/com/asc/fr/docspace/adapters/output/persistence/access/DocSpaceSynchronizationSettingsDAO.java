package com.asc.fr.docspace.adapters.output.persistence.access;

import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationSettingsEntity;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.session.DAOSession;

public class DocSpaceSynchronizationSettingsDAO
    extends BaseDAO<DocSpaceSynchronizationSettingsEntity> {
  public DocSpaceSynchronizationSettingsDAO(DAOSession session) {
    super(session);
  }

  @Override
  protected Class<DocSpaceSynchronizationSettingsEntity> getEntityClass() {
    return DocSpaceSynchronizationSettingsEntity.class;
  }
}

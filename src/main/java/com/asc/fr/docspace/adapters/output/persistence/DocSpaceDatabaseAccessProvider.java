package com.asc.fr.docspace.adapters.output.persistence;

import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceAccountDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSavedTenantDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSynchronizationEntryDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceSynchronizationSettingsDAO;
import com.asc.fr.docspace.adapters.output.persistence.access.DocSpaceTenantDAO;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceAccountEntity;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSavedTenantEntity;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationEntryEntity;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceSynchronizationSettingsEntity;
import com.asc.fr.docspace.adapters.output.persistence.entity.DocSpaceTenantEntity;
import com.fr.plugin.db.AbstractDBAccessProvider;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.db.accessor.DBAccessor;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.dao.DAOProvider;
import com.fr.stable.db.entity.BaseEntity;

@FunctionRecorder
public class DocSpaceDatabaseAccessProvider extends AbstractDBAccessProvider {
  private static volatile DBAccessor accessor;

  public static DBAccessor accessor() {
    return accessor;
  }

  private static <E extends BaseEntity> DAOProvider<E> dao(
      Class<E> entity, Class<? extends BaseDAO<E>> dao) {
    return new DAOProvider<E>() {
      @Override
      public Class<E> getEntityClass() {
        return entity;
      }

      @Override
      public Class<? extends BaseDAO<E>> getDAOClass() {
        return dao;
      }
    };
  }

  @Override
  public DAOProvider<?>[] registerDAO() {
    return new DAOProvider[] {
      dao(DocSpaceAccountEntity.class, DocSpaceAccountDAO.class),
      dao(DocSpaceTenantEntity.class, DocSpaceTenantDAO.class),
      dao(DocSpaceSavedTenantEntity.class, DocSpaceSavedTenantDAO.class),
      dao(DocSpaceSynchronizationEntryEntity.class, DocSpaceSynchronizationEntryDAO.class),
      dao(DocSpaceSynchronizationSettingsEntity.class, DocSpaceSynchronizationSettingsDAO.class)
    };
  }

  @Override
  public void onDBAvailable(DBAccessor dbAccessor) {
    accessor = dbAccessor;
  }
}

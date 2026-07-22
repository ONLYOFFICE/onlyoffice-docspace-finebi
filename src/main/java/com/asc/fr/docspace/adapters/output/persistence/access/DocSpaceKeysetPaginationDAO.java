package com.asc.fr.docspace.adapters.output.persistence.access;

import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.fr.stable.db.dao.BaseDAO;
import com.fr.stable.db.entity.BaseEntity;
import com.fr.stable.query.QueryFactory;
import com.fr.stable.query.condition.QueryCondition;
import com.fr.stable.query.restriction.Restriction;
import com.fr.stable.query.restriction.RestrictionFactory;
import java.io.IOException;
import java.util.List;

/** Keyset pagination so bulk reads and deletes never load a whole table into memory. */
public final class DocSpaceKeysetPaginationDAO {
  public static final int PAGE_SIZE = 100;

  private DocSpaceKeysetPaginationDAO() {}

  /**
   * A page of at most {@code limit} rows with {@code id > afterId}, optionally further filtered.
   */
  public static QueryCondition byId(String afterId, int limit, Restriction filter) {
    Restriction keyset = RestrictionFactory.gt("id", afterId == null ? "" : afterId);
    Restriction where = filter == null ? keyset : RestrictionFactory.and(keyset, filter);
    return QueryFactory.create().addRestriction(where).addSort("id", true).count(limit);
  }

  /**
   * Deletes every row of {@code daoClass}'s table, one {@link #PAGE_SIZE} page per transaction so a
   * huge table never loads at once (nor accumulates in a single Hibernate session).
   */
  public static <E extends BaseEntity, D extends BaseDAO<E>> void deleteAll(
      IUnitOfWork uow, Class<D> clazz) throws IOException {
    boolean more = true;
    while (more) {
      more =
          uow.write(
              ctx -> {
                D access = ctx.getDAO(clazz);
                List<E> page = access.find(byId("", PAGE_SIZE, null));
                for (E row : page) access.remove(row.getId());
                return page.size() == PAGE_SIZE;
              });
    }
  }
}

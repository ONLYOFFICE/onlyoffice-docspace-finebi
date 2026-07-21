package com.asc.fr.docspace.adapters.output.persistence;

import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.fr.stable.db.accessor.DBAccessor;
import com.fr.stable.db.dao.DAOContext;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;

/**
 * FineDB-backed {@link IUnitOfWork}.
 *
 * <h2>Create</h2>
 *
 * <pre>{@code
 * IUnitOfWork uow = FineUnitOfWork.builder().build();
 * // tests / custom accessor:
 * IUnitOfWork uow = FineUnitOfWork.builder()
 *     .accessor(() -> testDbAccessor)
 *     .build();
 * }</pre>
 *
 * <h2>Mental model</h2>
 *
 * <ul>
 *   <li>{@link #query} — open a <b>read</b> session, run the lambda, close. No write transaction.
 *       Failures degrade to {@link Optional#empty()}.
 *   <li>{@link #write} — open a session, {@code beginTransaction}, run the lambda, {@code commit}
 *       on success or {@code rollback} on {@link Exception}, then close. That is one atomic unit.
 * </ul>
 *
 * FineBI injects the default accessor via {@link DocSpaceDatabaseAccessProvider} ({@code
 * onDBAvailable}). Until then, queries return empty and writes throw.
 *
 * <h2>Getting a DAO</h2>
 *
 * <pre>{@code
 * uow.query(ctx -> ctx.getDAO(DocSpaceAccountDAO.class).getById(username));
 *
 * uow.write(ctx -> {
 *     ctx.getDAO(DocSpaceTenantDAO.class).addOrUpdate(tenant);
 *     return tenant;
 * });
 * }</pre>
 *
 * <h2>Atomicity</h2>
 *
 * <b>Atomic:</b> everything inside a single {@code write(...)} lambda (multiple DAO calls, across
 * entity types).
 *
 * <pre>{@code
 * uow.write(ctx -> {
 *     ctx.getDAO(DocSpaceTenantDAO.class).addOrUpdate(tenant);
 *     ctx.getDAO(DocSpaceSynchronizationSettingsDAO.class).addOrUpdate(settings);
 *     return null;
 * });
 * }</pre>
 *
 * <b>Not atomic:</b> separate {@code write}/{@code query} calls — each is its own session/TX. Fold
 * read-modify-write into one {@code write} when needed.
 *
 * <h2>Do not nest</h2>
 *
 * Never call {@code query}/{@code write} from inside another lambda (including via a repository
 * that uses this UoW). Pass {@link Context} down instead.
 *
 * <h2>Errors</h2>
 *
 * <ul>
 *   <li>{@code query} — catches {@link Throwable}, logs, returns empty.
 *   <li>{@code write} — wraps failure in {@link IOException}; TX already rolled back.
 * </ul>
 */
@Builder
public final class FineUnitOfWork implements IUnitOfWork {
  @Builder.Default
  private final Supplier<DBAccessor> accessor = DocSpaceDatabaseAccessProvider::accessor;

  private static Context adapter(DAOContext platformCtx) {
    return platformCtx::getDAO;
  }

  @Override
  public <T> Optional<T> query(@NonNull Work<T> work) {
    DBAccessor database = accessor.get();
    if (database == null) return Optional.empty();
    try {
      return Optional.ofNullable(database.runQueryAction(ctx -> work.execute(adapter(ctx))));
    } catch (Throwable t) {
      return Optional.empty();
    }
  }

  @Override
  public <T> T write(@NonNull Work<T> work) throws IOException {
    DBAccessor database = accessor.get();
    if (database == null) throw new IOException("FineBI's DBAccessProvider not initialized");
    try {
      return database.runDMLAction(ctx -> work.execute(adapter(ctx)));
    } catch (Throwable t) {
      throw new IOException("Could not write to FineBI's DB", t);
    }
  }

  @Override
  public void write(@NonNull VoidWork work) throws IOException {
    write(
        ctx -> {
          work.execute(ctx);
          return null;
        });
  }
}

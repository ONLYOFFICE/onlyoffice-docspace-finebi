package com.asc.fr.docspace.domain;

import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import java.io.IOException;
import java.util.List;

/**
 * Stores DocSpace and FineBI dataset links. A link is identified by the FineBI dataset UUID ({@code
 * tableId}), so a single DocSpace file imported into several folders is tracked as one link per
 * dataset rather than a single overwriting entry.
 */
public interface SynchronizationLinkRegistry {
  /** Upserts the link keyed by {@code entry.getTableId()}. */
  void put(FileSynchronizationRecord entry) throws IOException;

  /** Every dataset link sourced from the given DocSpace file. */
  List<FileSynchronizationRecord> findByFile(String fileId);

  /** Removes a single dataset link by its FineBI dataset UUID. */
  void remove(String tableId) throws IOException;

  /** Removes every dataset link sourced from the given DocSpace file. */
  void removeByFile(String fileId) throws IOException;

  /** Removes every link. Used when the tenant is reset. */
  void removeAll() throws IOException;

  List<FileSynchronizationRecord> staleLinks(long reconciledBefore, String afterTableId, int limit);

  /**
   * A page of links ordered by dataset UUID: {@code offset} rows skipped, at most {@code limit}
   * returned.
   */
  List<FileSynchronizationRecord> listLinks(int offset, int limit);
}

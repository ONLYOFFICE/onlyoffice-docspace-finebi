package com.asc.fr.docspace.domain;

import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import java.io.IOException;
import java.util.Map;

public interface SynchronizationService {
  void put(String fileId, FileSynchronizationRecord entry) throws IOException;

  FileSynchronizationRecord find(String fileId);

  void remove(String fileId) throws IOException;

  Map<String, FileSynchronizationRecord> entries();

  void storeCallbackUrl(String url) throws IOException;

  String loadCallbackUrl();

  String ensureSecret() throws IOException;

  String loadSecret();
}

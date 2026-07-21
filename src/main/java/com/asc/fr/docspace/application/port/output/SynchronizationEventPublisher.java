package com.asc.fr.docspace.application.port.output;

public interface SynchronizationEventPublisher {
  void datasetUpdated(String tableName);

  void tenantReset();
}

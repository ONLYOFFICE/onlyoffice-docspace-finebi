package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.application.port.input.transfer.SynchronizationCommand;

public interface SynchronizationService {
  void schedule(SynchronizationCommand command);
}

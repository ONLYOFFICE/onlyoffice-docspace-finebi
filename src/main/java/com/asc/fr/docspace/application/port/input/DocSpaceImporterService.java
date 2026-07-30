package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.application.port.input.transfer.ImportFileCommand;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;

public interface DocSpaceImporterService {
  int MAX_FILE_BYTES = 50 * 1024 * 1024;
  int MAX_SHEETS = 50;

  int importFile(ImportFileCommand command, FineSession session) throws IOException;
}

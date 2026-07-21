package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;

public interface FineExportService {
  byte[] downloadExport(String operationId, FineSession session) throws IOException;
}

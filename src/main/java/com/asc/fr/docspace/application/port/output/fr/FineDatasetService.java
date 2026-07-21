package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;

public interface FineDatasetService {
  String createDataset(FineCreateDatasetCommand command, FineSession session) throws IOException;

  void replaceDataset(
      FineReplaceDatasetCommand command, FineSession session, FineAttachment attachment)
      throws IOException;

  void refreshDataset(FineRefreshDatasetCommand command, FineSession session);
}

package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.application.exception.DatasetAbsentException;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineDatasetLocation;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceOutcome;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineCreatedDataset;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface FineDatasetService {
  List<FineCreatedDataset> createDatasets(FineCreateDatasetCommand command, FineSession session)
      throws IOException;

  default void replaceDataset(
      FineReplaceDatasetCommand command, FineSession session, FineAttachment attachment)
      throws IOException {
    FineReplaceOutcome outcome =
        replaceDatasets(Collections.singletonList(command), session, attachment).get(0);
    if (outcome.getStatus() == FineReplaceOutcome.Status.ABSENT)
      throw new DatasetAbsentException(command.getTableId());
    if (outcome.getStatus() == FineReplaceOutcome.Status.FAILED)
      throw new IOException(outcome.getDetail());
  }

  List<FineReplaceOutcome> replaceDatasets(
      List<FineReplaceDatasetCommand> commands, FineSession session, FineAttachment attachment);

  void refreshDataset(FineRefreshDatasetCommand command, FineSession session);

  Map<String, FineDatasetLocation> locateDatasets(Collection<String> tableIds, FineSession session)
      throws IOException;
}

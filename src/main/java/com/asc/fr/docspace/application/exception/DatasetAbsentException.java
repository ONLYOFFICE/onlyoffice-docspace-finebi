package com.asc.fr.docspace.application.exception;

import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import java.io.IOException;

/**
 * Thrown by {@link FineDatasetService#replaceDataset} when FineBI itself reports that the target
 * dataset no longer exists (errorCode 61310034 / "FineTableAbsentException").
 *
 * <p>This is the authoritative "deleted in FineBI" signal for the CLEANUP SEMANTICS in
 * DefaultResyncService: unlike probing list endpoints (which vary across FineBI versions and proved
 * unavailable in practice), the update API always knows whether its target exists. Any other {@link
 * IOException} from the gateway is treated as transient.
 */
public class DatasetAbsentException extends IOException {
  public DatasetAbsentException(String tableUuid) {
    super("FineBI dataset " + tableUuid + " no longer exists");
  }
}

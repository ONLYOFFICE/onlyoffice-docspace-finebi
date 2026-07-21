package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** JSON body of the import endpoint: which DocSpace file to import and where to put it. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ImportRequest {
  private String fileId;
  private String filename;
  private String viewUrl;
  private String folderId;
}

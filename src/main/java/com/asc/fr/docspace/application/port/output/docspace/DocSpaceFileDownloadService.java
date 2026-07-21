package com.asc.fr.docspace.application.port.output.docspace;

import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileCommand;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileFromUrlCommand;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import java.io.IOException;

public interface DocSpaceFileDownloadService {
  DocSpaceRawFile download(
      DocSpaceDownloadFileCommand command, DocSpaceAccountCredentials credentials)
      throws IOException;

  DocSpaceRawFile downloadFromUrl(
      DocSpaceDownloadFileFromUrlCommand command, DocSpaceAccountCredentials credentials)
      throws IOException;
}

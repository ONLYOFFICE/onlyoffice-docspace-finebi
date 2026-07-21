package com.asc.fr.docspace.application.port.output.docspace;

import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceUploadFileCommand;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import java.io.IOException;

public interface DocSpaceFileUploadService {
  DocSpaceUploadedFile upload(
      DocSpaceUploadFileCommand command, DocSpaceAccountCredentials credentials) throws IOException;
}

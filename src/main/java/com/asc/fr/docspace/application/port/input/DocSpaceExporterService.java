package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.application.port.input.transfer.ExportFileCommand;
import com.asc.fr.docspace.application.port.input.transfer.UploadFileCommand;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;

public interface DocSpaceExporterService {
  DocSpaceUploadedFile upload(UploadFileCommand command) throws IOException;

  DocSpaceUploadedFile export(ExportFileCommand command, FineSession session) throws IOException;
}

package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.application.port.output.fr.transfer.FineUploadAttachmentCommand;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;

public interface FineAttachmentService {
  FineAttachment uploadAttachment(FineUploadAttachmentCommand command, FineSession session)
      throws IOException;
}

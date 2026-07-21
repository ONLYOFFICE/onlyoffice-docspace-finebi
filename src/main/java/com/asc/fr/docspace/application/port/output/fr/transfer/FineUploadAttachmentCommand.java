package com.asc.fr.docspace.application.port.output.fr.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FineUploadAttachmentCommand implements Command {
  private final String fileName;
  private final byte[] content;

  @Override
  public boolean valid() {
    if (fileName == null || fileName.trim().isEmpty()) return false;

    return content != null && content.length >= 1;
  }
}

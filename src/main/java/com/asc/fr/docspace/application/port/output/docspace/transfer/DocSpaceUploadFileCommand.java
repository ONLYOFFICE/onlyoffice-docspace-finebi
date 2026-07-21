package com.asc.fr.docspace.application.port.output.docspace.transfer;

import com.asc.fr.docspace.application.port.Command;
import com.asc.fr.docspace.domain.common.URL;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocSpaceUploadFileCommand implements Command {
  private final URL docSpaceUrl;
  private final String folderId;
  private final String fileName;
  private final byte[] content;

  @Override
  public boolean valid() {
    if (docSpaceUrl == null) return false;

    if (folderId == null || folderId.trim().isEmpty()) return false;

    if (fileName == null || fileName.trim().isEmpty()) return false;

    return content != null && content.length >= 1;
  }
}

package com.asc.fr.docspace.application.port.input.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImportFileCommand implements Command {
  private final String userName;
  private final String fileId;
  private final String fileName;
  private final String viewUrl;
  private final String folderId;
  private final String callbackUrl;

  @Override
  public boolean valid() {
    if (userName == null || userName.trim().isEmpty()) return false;

    if (fileId == null || fileId.trim().isEmpty()) return false;

    return callbackUrl != null && !callbackUrl.trim().isEmpty();
  }
}

package com.asc.fr.docspace.application.port.input.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileCommand implements Command {
  private final String userName;
  private final String fileName;
  private final byte[] content;

  public UploadFileCommand(String userName, String fileName, byte[] content) {
    this.userName = userName;
    this.fileName = fileName;
    this.content = content;
  }

  @Override
  public boolean valid() {
    if (userName == null || userName.trim().isEmpty()) return false;

    if (fileName == null || fileName.trim().isEmpty()) return false;

    return content != null && content.length >= 1;
  }
}

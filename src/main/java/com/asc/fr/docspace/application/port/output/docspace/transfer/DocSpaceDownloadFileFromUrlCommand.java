package com.asc.fr.docspace.application.port.output.docspace.transfer;

import com.asc.fr.docspace.application.port.Command;
import com.asc.fr.docspace.domain.common.URL;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocSpaceDownloadFileFromUrlCommand implements Command {
  private final URL docSpaceUrl;
  private final String viewUrl;
  private final String fileNameHint;

  @Override
  public boolean valid() {
    if (docSpaceUrl == null) return false;

    return viewUrl != null && !viewUrl.trim().isEmpty();
  }
}

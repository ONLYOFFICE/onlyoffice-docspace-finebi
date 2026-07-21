package com.asc.fr.docspace.adapters.input.web.export.transfer;

import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import lombok.Getter;

/** Result payload of the upload/trigger endpoints: where the file landed. */
@Getter
public final class UploadedFileResponse extends OkResponse {
  private final String filename;
  private final String fileId;
  private final String folderId;

  private UploadedFileResponse(String filename, String fileId, String folderId) {
    super(true);
    this.filename = filename;
    this.fileId = fileId;
    this.folderId = folderId;
  }

  public static UploadedFileResponse of(DocSpaceUploadedFile file) {
    return new UploadedFileResponse(file.getFilename(), file.getFileId(), file.getFolderId());
  }
}

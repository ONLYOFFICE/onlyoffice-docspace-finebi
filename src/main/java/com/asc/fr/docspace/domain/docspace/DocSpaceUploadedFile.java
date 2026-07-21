package com.asc.fr.docspace.domain.docspace;

import com.asc.fr.docspace.domain.DomainValidator;
import com.asc.fr.docspace.domain.exception.InvalidUploadedFileException;
import lombok.Getter;

@Getter
public final class DocSpaceUploadedFile {
  private final String fileId;
  private final String filename;
  private final String folderId;

  public DocSpaceUploadedFile(String fileId, String filename, String folderId) {
    this.fileId =
        DomainValidator.requirePresent(
            fileId, () -> new InvalidUploadedFileException("File id must not be blank"));
    this.filename =
        DomainValidator.requirePresent(
            filename, () -> new InvalidUploadedFileException("File name must not be blank"));
    this.folderId =
        DomainValidator.requirePresent(
            folderId, () -> new InvalidUploadedFileException("Folder id must not be blank"));
  }
}

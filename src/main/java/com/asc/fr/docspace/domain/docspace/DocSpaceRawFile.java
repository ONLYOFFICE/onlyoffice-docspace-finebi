package com.asc.fr.docspace.domain.docspace;

import com.asc.fr.docspace.domain.DomainValidator;
import com.asc.fr.docspace.domain.exception.InvalidRawFileException;
import lombok.Getter;

@Getter
public final class DocSpaceRawFile {
  private final String fileName;
  private final byte[] rawContent;

  public DocSpaceRawFile(String fileName, byte[] rawContent) {
    this.fileName =
        DomainValidator.requirePresent(
            fileName, () -> new InvalidRawFileException("File name must not be blank"));
    if (rawContent == null || rawContent.length == 0)
      throw new InvalidRawFileException("Raw content must not be empty");
    this.rawContent = rawContent;
  }
}

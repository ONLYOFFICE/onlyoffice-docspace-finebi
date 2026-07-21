package com.asc.fr.docspace.domain.fr;

import com.asc.fr.docspace.domain.DomainValidator;
import com.asc.fr.docspace.domain.exception.InvalidAttachmentException;
import lombok.Getter;

@Getter
public final class FineAttachment {
  private final String attachId;

  public FineAttachment(String attachId) {
    this.attachId =
        DomainValidator.requirePresent(
            attachId, () -> new InvalidAttachmentException("Attachment id must not be blank"));
  }
}

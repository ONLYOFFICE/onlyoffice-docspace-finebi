package com.asc.fr.docspace.domain.fr;

import com.asc.fr.docspace.domain.DomainValidator;
import com.asc.fr.docspace.domain.exception.InvalidFolderException;
import lombok.Getter;

@Getter
public final class FineFolder {
  private final String id;
  private final String name;

  public FineFolder(String id, String name) {
    this.id =
        DomainValidator.requirePresent(
            id, () -> new InvalidFolderException("Id must not be blank"));
    this.name =
        DomainValidator.requirePresent(
            name, () -> new InvalidFolderException("Name must not be blank"));
  }
}

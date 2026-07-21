package com.asc.fr.docspace.adapters.output.service;

import com.asc.fr.docspace.application.exception.EncryptorException;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.fr.security.encryption.storage.StorageEncryptors;

public final class FineStorageEncryptorsService implements FineEncryptionService {
  public String encrypt(String value) throws EncryptorException {
    if (value == null || value.isEmpty())
      throw new EncryptorException("Value must not be empty of blank to encrypt it");

    return StorageEncryptors.getInstance().encrypt(value);
  }

  public String decrypt(String stored) throws EncryptorException {
    if (stored == null || stored.isEmpty())
      throw new EncryptorException("Stored value must not be empty of blank to decrypt it");

    try {
      return StorageEncryptors.getInstance().decrypt(stored);
    } catch (Exception e) {
      throw new EncryptorException(e.getMessage());
    }
  }
}

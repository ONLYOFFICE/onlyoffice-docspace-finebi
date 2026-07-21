package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.application.exception.EncryptorException;

public interface FineEncryptionService {
  String encrypt(String value) throws EncryptorException;

  String decrypt(String stored) throws EncryptorException;
}

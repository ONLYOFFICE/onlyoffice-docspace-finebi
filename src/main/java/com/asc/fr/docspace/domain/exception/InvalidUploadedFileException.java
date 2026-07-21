package com.asc.fr.docspace.domain.exception;

public class InvalidUploadedFileException extends RuntimeException {
  public InvalidUploadedFileException(String message) {
    super(message);
  }
}

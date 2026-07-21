package com.asc.fr.docspace.domain.exception;

public class InvalidUrlException extends RuntimeException {
  public InvalidUrlException(String message) {
    super(message);
  }
}

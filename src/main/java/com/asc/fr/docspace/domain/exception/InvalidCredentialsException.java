package com.asc.fr.docspace.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
  private final String code;

  public InvalidCredentialsException(String message) {
    this(message, null);
  }

  public InvalidCredentialsException(String message, String code) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}

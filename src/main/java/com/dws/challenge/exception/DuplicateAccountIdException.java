package com.dws.challenge.exception;

public class DuplicateAccountIdException extends RuntimeException {

  public DuplicateAccountIdException(String message) {
    super(message);
  }
}

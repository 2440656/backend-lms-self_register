package com.cognizant.lms.userservice.exception;

public class SFTPConnectionException extends RuntimeException {

  public SFTPConnectionException(String message) {
    super(message);
  }

  public SFTPConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}

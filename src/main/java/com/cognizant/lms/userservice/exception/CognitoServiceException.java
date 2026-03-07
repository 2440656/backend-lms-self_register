package com.cognizant.lms.userservice.exception;

import lombok.Getter;

@Getter
public class CognitoServiceException extends RuntimeException {
  private final String errorCode;

  public CognitoServiceException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public CognitoServiceException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

}

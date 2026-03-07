package com.cognizant.lms.userservice.exception;

import com.cognizant.lms.userservice.dto.FileUploadResponse;

public class FileProcessingException extends RuntimeException {

  private FileUploadResponse fileUploadResponse;

  public FileProcessingException(String message) {
    super(message);
  }

  public FileProcessingException(String message, FileUploadResponse fileUploadResponse) {
    super(message);
    this.fileUploadResponse = fileUploadResponse;
  }

  public FileUploadResponse getFileUploadResponse() {
    return fileUploadResponse;
  }
}

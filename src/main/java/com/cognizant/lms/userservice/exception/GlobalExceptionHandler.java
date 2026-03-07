package com.cognizant.lms.userservice.exception;

import com.cognizant.lms.userservice.dto.HttpResponse;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FileStorageException.class)
  public ResponseEntity<HttpResponse> handleFileStorageFileException(
      FileStorageException exception) {

    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ExceptionHandler(DataBaseException.class)
  public ResponseEntity<HttpResponse> handleDataBaseException(DataBaseException exception) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<String> handleFileNotFoundException(FileNotFoundException ex,
                                                            WebRequest request) {
    return new ResponseEntity<>("File not found: " + ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MalformedURLException.class)
  public ResponseEntity<String> handleMalformedURLException(MalformedURLException ex,
                                                            WebRequest request) {
    return new ResponseEntity<>("Malformed URL: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FileValidationException.class)
  public ResponseEntity<HttpResponse> handleFileValidationException(
      FileValidationException exception) {

    return buildErrorResponse(null, HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<HttpResponse> handleUserNotFoundException(UserNotFoundException exception) {
    return buildErrorResponse(null, HttpStatus.NOT_FOUND, exception.getMessage());
  }
  @ExceptionHandler(UserSettingAlreadyFoundException.class)
  public ResponseEntity<HttpResponse> handleUserSettingAlreadyFoundException(UserSettingAlreadyFoundException exception) {
    return buildErrorResponse(null, HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(S3Exception.class)
  public ResponseEntity<HttpResponse> handleS3Exception(S3Exception exception) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR,
        exception.awsErrorDetails().errorMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<HttpResponse> handleGlobalException(Exception exception) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred " + exception.getMessage());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<HttpResponse> handleInternalServerError(Exception exception,
                                                                WebRequest request) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred " + exception.getMessage());
  }

  private ResponseEntity<HttpResponse> buildErrorResponse(Object data, HttpStatus status,
                                                          String errorMessage) {
    HttpResponse response = new HttpResponse();
    response.setStatus(status.value());
    response.setError(errorMessage);
    response.setData(data);
    return new ResponseEntity<>(response, status);
  }

  @ExceptionHandler(FileProcessingException.class)
  public ResponseEntity<HttpResponse> fileProcessingException(FileProcessingException exception) {
    return buildErrorResponse(exception.getFileUploadResponse(), HttpStatus.BAD_REQUEST,
        exception.getMessage());
  }

  @ExceptionHandler(CognitoServiceException.class)
  public ResponseEntity<HttpResponse> cognitoServiceException(CognitoServiceException exception) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ExceptionHandler(UserFilterSortException.class)
  public ResponseEntity<HttpResponse> handleUserFilterSortException(
      UserFilterSortException exception) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<HttpResponse> handleValidationException(ValidationException ex,
                                                                WebRequest request) {
    return buildErrorResponse(null, HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(NoLogFilesFound.class)
  public ResponseEntity<HttpResponse> handleNoLogFilesFound(NoLogFilesFound exception) {
    return buildErrorResponse(null, HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(TokenException.class)
  public ResponseEntity<HttpResponse> handleTokenException(TokenException ex) {
    return buildErrorResponse(null, HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(SkillLookupNotFoundException.class)
  public ResponseEntity<HttpResponse> handleSkillLookupNotFoundException(
      SkillLookupNotFoundException exception) {
    return buildErrorResponse(null, HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(PopularLinkLimitException.class)
  public ResponseEntity<HttpResponse> handlePopularLinkLimitException(PopularLinkLimitException exception) {
    return buildErrorResponse(null, HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(PopularLinkNotFoundException.class)
  public ResponseEntity<HttpResponse> handlePopularLinkNotFoundException(PopularLinkNotFoundException exception) {
    return buildErrorResponse(null, HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(PopularLinkDuplicateIndexException.class)
  public ResponseEntity<HttpResponse> handlePopularLinkDuplicateIndexException(PopularLinkDuplicateIndexException exception) {
    return buildErrorResponse(null, HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(SFTPConnectionException.class)
  public ResponseEntity<HttpResponse> handleSFTPConnectionException(SFTPConnectionException exception) {
    return buildErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ExceptionHandler(SFTPIntegrationAlreadyExistException.class)
  public ResponseEntity<HttpResponse> handleSFTPIntegrationAlreadyExistException(SFTPIntegrationAlreadyExistException exception) {
    return buildErrorResponse(null, HttpStatus.OK, exception.getMessage());
  }
}

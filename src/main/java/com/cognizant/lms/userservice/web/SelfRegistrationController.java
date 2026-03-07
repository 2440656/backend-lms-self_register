package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.EmailOtpActionResponse;
import com.cognizant.lms.userservice.dto.EmailOtpResendRequest;
import com.cognizant.lms.userservice.dto.EmailOtpVerificationRequest;
import com.cognizant.lms.userservice.dto.EmailSelfRegistrationRequest;
import com.cognizant.lms.userservice.dto.EmailSelfRegistrationResponse;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.exception.ValidationException;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@RestController
@RequestMapping("api/v1/auth")
@CrossOrigin(
    origins = {"http://localhost:3000", "http://127.0.0.1:3000"},
    allowedHeaders = "*",
    methods = {
      org.springframework.web.bind.annotation.RequestMethod.POST,
      org.springframework.web.bind.annotation.RequestMethod.OPTIONS
    })
public class SelfRegistrationController {

  private final UserService userService;

  public SelfRegistrationController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register/email")
  public ResponseEntity<HttpResponse> registerByEmail(
      @RequestBody EmailSelfRegistrationRequest request,
      @RequestHeader(value = Constants.TENANT_HEADER, required = false) String tenantIdentifier) {
    HttpResponse response = new HttpResponse();
    try {
      ResponseEntity<HttpResponse> tenantValidationResult = setTenantContext(tenantIdentifier);
      if (tenantValidationResult != null) {
        return tenantValidationResult;
      }
      EmailSelfRegistrationResponse registrationResponse = userService.registerUserByEmail(request);
      response.setData(registrationResponse);
      response.setStatus(HttpStatus.CREATED.value());
      response.setError(null);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (ValidationException e) {
      log.error("Validation failed for self registration: {}", e.getMessage());
      response.setData(null);
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError(e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      log.error("Error while self registration by email", e);
      response.setData(null);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Unable to register user");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/register/email/verify-otp")
  public ResponseEntity<HttpResponse> verifyEmailOtp(
      @RequestBody EmailOtpVerificationRequest request,
      @RequestHeader(value = Constants.TENANT_HEADER, required = false) String tenantIdentifier) {
    HttpResponse response = new HttpResponse();
    try {
      ResponseEntity<HttpResponse> tenantValidationResult = setTenantContext(tenantIdentifier);
      if (tenantValidationResult != null) {
        return tenantValidationResult;
      }
      EmailOtpActionResponse verificationResponse = userService.verifyRegistrationOtp(request);
      response.setData(verificationResponse);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (ValidationException e) {
      log.error("OTP verification validation failed: {}", e.getMessage());
      response.setData(null);
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError(e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      log.error("Error while verifying OTP", e);
      response.setData(null);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Unable to verify OTP");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/email/verify")
  public ResponseEntity<HttpResponse> verifyEmailOtpAlias(
      @RequestBody EmailOtpVerificationRequest request,
      @RequestHeader(value = Constants.TENANT_HEADER, required = false) String tenantIdentifier) {
    return verifyEmailOtp(request, tenantIdentifier);
  }

  @PostMapping("/register/email/resend-otp")
  public ResponseEntity<HttpResponse> resendEmailOtp(
      @RequestBody EmailOtpResendRequest request,
      @RequestHeader(value = Constants.TENANT_HEADER, required = false) String tenantIdentifier) {
    HttpResponse response = new HttpResponse();
    try {
      ResponseEntity<HttpResponse> tenantValidationResult = setTenantContext(tenantIdentifier);
      if (tenantValidationResult != null) {
        return tenantValidationResult;
      }
      EmailOtpActionResponse resendResponse = userService.resendRegistrationOtp(request);
      response.setData(resendResponse);
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (ValidationException e) {
      log.error("OTP resend validation failed: {}", e.getMessage());
      response.setData(null);
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError(e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      log.error("Error while resending OTP", e);
      response.setData(null);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("Unable to resend OTP");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/email/resend")
  public ResponseEntity<HttpResponse> resendEmailOtpAlias(
      @RequestBody EmailOtpResendRequest request,
      @RequestHeader(value = Constants.TENANT_HEADER, required = false) String tenantIdentifier) {
    return resendEmailOtp(request, tenantIdentifier);
  }

  private ResponseEntity<HttpResponse> setTenantContext(String tenantIdentifier) {
    HttpResponse response = new HttpResponse();
    if (tenantIdentifier == null || tenantIdentifier.trim().isEmpty()) {
      response.setData(null);
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError("x-tenant-id header is required");
      return ResponseEntity.badRequest().body(response);
    }

    TenantDTO tenantDetails = userService.getTenantDetails(tenantIdentifier.trim());
    if (tenantDetails == null) {
      response.setData(null);
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setError("Invalid tenant identifier");
      return ResponseEntity.badRequest().body(response);
    }

    TenantUtil.setTenantDetails(tenantDetails);
    return null;
  }
}

package com.cognizant.lms.userservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.EmailOtpActionResponse;
import com.cognizant.lms.userservice.dto.EmailOtpResendRequest;
import com.cognizant.lms.userservice.dto.EmailOtpVerificationRequest;
import com.cognizant.lms.userservice.dto.EmailSelfRegistrationRequest;
import com.cognizant.lms.userservice.dto.EmailSelfRegistrationResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.exception.ValidationException;
import com.cognizant.lms.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SelfRegistrationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        SelfRegistrationController controller = new SelfRegistrationController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

  @Test
  void registerByEmail_shouldReturnBadRequestWhenTenantHeaderMissing() throws Exception {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");

    mockMvc.perform(post("/api/v1/auth/register/email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("x-tenant-id header is required"));
  }

  @Test
  void registerByEmail_shouldReturnBadRequestWhenTenantIsInvalid() throws Exception {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");
    when(userService.getTenantDetails("tenant.acme.com")).thenReturn(null);

    mockMvc.perform(post("/api/v1/auth/register/email")
            .header(Constants.TENANT_HEADER, "tenant.acme.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Invalid tenant identifier"));
  }

  @Test
  void registerByEmail_shouldReturnCreatedWhenRequestIsValid() throws Exception {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");
    TenantDTO tenant = new TenantDTO("t-2", "tenant.acme.com", "Acme", "skillspring credentials",
        "portal", "client", "issuer", "cert");
    when(userService.getTenantDetails("tenant.acme.com")).thenReturn(tenant);
    when(userService.registerUserByEmail(any())).thenReturn(
        new EmailSelfRegistrationResponse("usr-123", true));

    mockMvc.perform(post("/api/v1/auth/register/email")
            .header(Constants.TENANT_HEADER, "tenant.acme.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.userId").value("usr-123"))
        .andExpect(jsonPath("$.data.emailVerificationRequired").value(true));
  }

  @Test
  void registerByEmail_shouldReturnBadRequestWhenValidationFails() throws Exception {
    EmailSelfRegistrationRequest request = new EmailSelfRegistrationRequest(
        "John", "Doe", "john.doe@example.com", "India", "Institute", "Strong@123",
        "Strong@123");
    TenantDTO tenant = new TenantDTO("t-2", "tenant.acme.com", "Acme", "skillspring credentials",
        "portal", "client", "issuer", "cert");
    when(userService.getTenantDetails("tenant.acme.com")).thenReturn(tenant);
    when(userService.registerUserByEmail(any())).thenThrow(new ValidationException("EmailId already exists"));

    mockMvc.perform(post("/api/v1/auth/register/email")
            .header(Constants.TENANT_HEADER, "tenant.acme.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EmailId already exists"));
  }

  @Test
  void verifyEmailOtp_shouldReturnOkWhenRequestIsValid() throws Exception {
    EmailOtpVerificationRequest request = new EmailOtpVerificationRequest(
        "john.doe@example.com", "123456");
    TenantDTO tenant = new TenantDTO("t-2", "tenant.acme.com", "Acme", "skillspring credentials",
        "portal", "client", "issuer", "cert");
    when(userService.getTenantDetails("tenant.acme.com")).thenReturn(tenant);
    when(userService.verifyRegistrationOtp(any())).thenReturn(
        new EmailOtpActionResponse("john.doe@example.com", true, "Email verification successful"));

    mockMvc.perform(post("/api/v1/auth/register/email/verify-otp")
            .header(Constants.TENANT_HEADER, "tenant.acme.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.success").value(true));
  }

  @Test
  void verifyEmailOtpAlias_shouldReturnOkWhenRequestIsValid() throws Exception {
    EmailOtpVerificationRequest request = new EmailOtpVerificationRequest(
        "john.doe@example.com", "123456");
    TenantDTO tenant = new TenantDTO("t-2", "tenant.acme.com", "Acme", "skillspring credentials",
        "portal", "client", "issuer", "cert");
    when(userService.getTenantDetails("tenant.acme.com")).thenReturn(tenant);
    when(userService.verifyRegistrationOtp(any())).thenReturn(
        new EmailOtpActionResponse("john.doe@example.com", true, "Email verification successful"));

    mockMvc.perform(post("/api/v1/auth/email/verify")
            .header(Constants.TENANT_HEADER, "tenant.acme.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.success").value(true));
  }

  @Test
  void resendEmailOtp_shouldReturnOkWhenRequestIsValid() throws Exception {
    EmailOtpResendRequest request = new EmailOtpResendRequest("john.doe@example.com");
    TenantDTO tenant = new TenantDTO("t-2", "tenant.acme.com", "Acme", "skillspring credentials",
        "portal", "client", "issuer", "cert");
    when(userService.getTenantDetails("tenant.acme.com")).thenReturn(tenant);
    when(userService.resendRegistrationOtp(any())).thenReturn(
        new EmailOtpActionResponse("john.doe@example.com", true, "OTP has been sent to your email"));

    mockMvc.perform(post("/api/v1/auth/register/email/resend-otp")
            .header(Constants.TENANT_HEADER, "tenant.acme.com")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.success").value(true));
  }

    @Test
    void resendEmailOtpAlias_shouldReturnOkWhenRequestIsValid() throws Exception {
        EmailOtpResendRequest request = new EmailOtpResendRequest("john.doe@example.com");
        TenantDTO tenant = new TenantDTO("t-2", "tenant.acme.com", "Acme", "skillspring credentials",
                "portal", "client", "issuer", "cert");
        when(userService.getTenantDetails("tenant.acme.com")).thenReturn(tenant);
        when(userService.resendRegistrationOtp(any())).thenReturn(
                new EmailOtpActionResponse("john.doe@example.com", true, "OTP has been sent to your email"));

        mockMvc.perform(post("/api/v1/auth/email/resend")
                        .header(Constants.TENANT_HEADER, "tenant.acme.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }
}

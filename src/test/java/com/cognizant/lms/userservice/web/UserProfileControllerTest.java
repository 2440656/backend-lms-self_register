package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.UserHomeProfileDto;
import com.cognizant.lms.userservice.dto.UserPersonalDetailsDto;
import com.cognizant.lms.userservice.service.UserService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileControllerTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private UserProfileController userProfileController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    AuthUser authUser = new AuthUser("test-user", "password", Collections.emptyList());
    authUser.setUserEmail("learner@example.com");

    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(authUser, null, authUser.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getHomeProfileDetails_success() {
    User user = User.builder()
        .pk("USER#1001")
        .firstName("Sanjana")
        .lastName("Shah")
        .emailId("learner@example.com")
        .build();

    when(userService.getUserByEmailId("learner@example.com", Constants.ACTIVE_STATUS))
        .thenReturn(user);

    ResponseEntity<HttpResponse> response = userProfileController.getHomeProfileDetails();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());

    UserHomeProfileDto dto = (UserHomeProfileDto) response.getBody().getData();
    assertEquals("USER#1001", dto.getUserId());
    assertEquals("Sanjana", dto.getFirstName());
    assertEquals("Shah", dto.getLastName());
    assertEquals("Sanjana Shah", dto.getDisplayName());

    verify(userService).getUserByEmailId("learner@example.com", Constants.ACTIVE_STATUS);
  }

  @Test
  void getPersonalDetails_success() {
    User user = User.builder()
        .pk("USER#1002")
        .firstName("Sanjana")
        .lastName("Shah")
        .emailId("learner@example.com")
        .country("India")
        .institutionName("XYZ Institute of Technology")
        .role("Product Owner")
        .build();

    when(userService.getUserByEmailId("learner@example.com", Constants.ACTIVE_STATUS))
        .thenReturn(user);

    ResponseEntity<HttpResponse> response = userProfileController.getPersonalDetails();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());

    UserPersonalDetailsDto dto = (UserPersonalDetailsDto) response.getBody().getData();
    assertEquals("USER#1002", dto.getUserId());
    assertEquals("Sanjana", dto.getFirstName());
    assertEquals("Shah", dto.getLastName());
    assertEquals("learner@example.com", dto.getEmailAddress());
    assertEquals("India", dto.getCountry());
    assertEquals("XYZ Institute of Technology", dto.getInstitutionName());
    assertEquals("Product Owner", dto.getCurrentRole());

    verify(userService).getUserByEmailId("learner@example.com", Constants.ACTIVE_STATUS);
  }
}

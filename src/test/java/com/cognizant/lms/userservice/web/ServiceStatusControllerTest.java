package com.cognizant.lms.userservice.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
@ExtendWith(MockitoExtension.class)
public class ServiceStatusControllerTest {

  @InjectMocks
  private ServiceStatusController serviceStatusController;

  @Test
  public void testStatus() {
    ResponseEntity<Void> response = serviceStatusController.status();
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}


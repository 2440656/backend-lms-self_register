package com.cognizant.lms.userservice.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.service.RoleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RoleControllerTest {

  @Mock
  private RoleService roleService;

  @InjectMocks
  private RoleController roleController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getRoles_returnsOkResponse() {
    List<RoleDto> mockRoles = List.of(new RoleDto());
    when(roleService.getRoles()).thenReturn(mockRoles);
    ResponseEntity<HttpResponse> responseEntity = roleController.getRoles();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(mockRoles, responseEntity.getBody().getData());
    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
    assertEquals(null, responseEntity.getBody().getError());
    verify(roleService, times(1)).getRoles();
  }

}

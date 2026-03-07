package com.cognizant.lms.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dto.RoleDto;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RoleServiceImplTest {

  @Mock
  private RoleDao roleDao;

  @InjectMocks
  private RoleServiceImpl roleServiceImpl;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getRoles_success() {
    List<RoleDto> mockRoles = Arrays.asList(
        new RoleDto("pk", "sk", "role1"),
        new RoleDto("pk2", "sk2", "role2"));
    when(roleDao.getRoles()).thenReturn(mockRoles);
    List<RoleDto> roles = roleServiceImpl.getRoles();
    assertNotNull(roles);
    assertEquals(2, roles.size());
    assertEquals("role1", roles.get(0).getName());
    assertEquals("role2", roles.get(1).getName());
    verify(roleDao, times(1)).getRoles();
  }

  @Test
  void getRoles_exception() {
    when(roleDao.getRoles()).thenThrow(new RuntimeException("Database error"));
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      roleServiceImpl.getRoles();
    });
    assertEquals("Database error", exception.getMessage());
    verify(roleDao, times(1)).getRoles();
  }
}

package com.cognizant.lms.userservice.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cognizant.lms.userservice.constants.Constants;

public class TokenUtilTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private ServletRequestAttributes mockAttributes;

  private TokenUtil tokenUtil;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    tokenUtil = new TokenUtil();

    when(mockAttributes.getRequest()).thenReturn(mockRequest);
    RequestContextHolder.setRequestAttributes(mockAttributes);
  }

  @Test
  public void testValidAuthorizationHeader_SanitizedValid() {
    String rawToken = "Bearer abc.def.ghi";
    String sanitizedToken = "Bearer abc.def.ghi";

    when(mockRequest.getHeader(Constants.AUTHORIZATION)).thenReturn(rawToken);

    try (MockedStatic<SanitizeUtil> mockedStatic = mockStatic(SanitizeUtil.class)) {
      mockedStatic.when(() -> SanitizeUtil.sanitizeData(rawToken)).thenReturn(sanitizedToken);

      String result = tokenUtil.getAuthorizationHeader();
      assertEquals(sanitizedToken, result);
    }
  }

  @Test
  public void testValidAuthorizationHeader_SanitizedEmpty() {
    String rawToken = "Bearer suspicious.token";

    when(mockRequest.getHeader(Constants.AUTHORIZATION)).thenReturn(rawToken);

    try (MockedStatic<SanitizeUtil> mockedStatic = mockStatic(SanitizeUtil.class)) {
      mockedStatic.when(() -> SanitizeUtil.sanitizeData(rawToken)).thenReturn("");

      String result = tokenUtil.getAuthorizationHeader();
      assertEquals("", result);
    }
  }

  @Test
  public void testMissingAuthorizationHeader() {
    when(mockRequest.getHeader(Constants.AUTHORIZATION)).thenReturn(null);

    String result = tokenUtil.getAuthorizationHeader();
    assertEquals("", result);
  }

  @Test
  public void testRequestAttributesNull_ThrowsException() {
    RequestContextHolder.setRequestAttributes(null);

    String result = tokenUtil.getAuthorizationHeader();
    assertEquals("", result);
  }
}
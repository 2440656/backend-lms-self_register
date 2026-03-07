package com.cognizant.lms.userservice.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class CookieUtilTest {

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private CookieUtil cookieUtil;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);
  }

  @Test
  public void testGetCookies() {
    Cookie[] cookies = {
        new Cookie("cookie1", "value1"),
        new Cookie("cookie2", "value2")
    };
    when(request.getCookies()).thenReturn(cookies);

    String result = CookieUtil.getCookies();
    assertEquals("cookie1=value1; cookie2=value2", result);
  }

  @Test
  public void testGetCookies_NoCookies() {
    when(request.getCookies()).thenReturn(null);

    String result = CookieUtil.getCookies();
    assertEquals("", result);
  }
}

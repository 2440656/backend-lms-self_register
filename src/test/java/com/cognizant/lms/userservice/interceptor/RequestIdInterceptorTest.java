package com.cognizant.lms.userservice.interceptor;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdInterceptorTest {

  private final RequestIdInterceptor interceptor = new RequestIdInterceptor();

  @AfterEach
  void clearThreadContext() {
    ThreadContext.clearAll();
  }

  @Test
  void preHandle_shouldUseTenantHeader_whenTenantUtilIsNotInitialized() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(Constants.TENANT_HEADER, "localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertEquals("localhost", ThreadContext.get("tenantCode"));
    assertTrue(ThreadContext.get("requestId") != null && !ThreadContext.get("requestId").isBlank());
  }

  @Test
  void preHandle_shouldUseUnknown_whenTenantNotInitializedAndHeaderMissing() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertEquals("unknown", ThreadContext.get("tenantCode"));
  }
}

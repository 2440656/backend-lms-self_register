package com.cognizant.lms.userservice.interceptor;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.utils.TenantUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestIdInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler) {
    String requestId = request.getHeader("X-Request-ID");
    if (requestId == null) {
      requestId = java.util.UUID.randomUUID().toString();
    }

    String tenantCode;
    try {
      tenantCode = TenantUtil.getTenantCode();
    } catch (IllegalStateException exception) {
      tenantCode = request.getHeader(Constants.TENANT_HEADER);
      if (tenantCode == null || tenantCode.isBlank()) {
        tenantCode = "unknown";
      }
    }

    ThreadContext.put("requestId", requestId);
    ThreadContext.put("tenantCode", tenantCode);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    ThreadContext.clearAll();
  }
}
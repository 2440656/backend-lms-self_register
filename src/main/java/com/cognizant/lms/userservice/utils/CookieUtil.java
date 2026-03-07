package com.cognizant.lms.userservice.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class CookieUtil {

  public static String getCookies() {
    HttpServletRequest currentRequest = ((ServletRequestAttributes)
        Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    Cookie[] cookies = currentRequest.getCookies();
    if (cookies != null) {
      return Arrays.stream(cookies)
          .map(cookie -> cookie.getName() + "=" + cookie.getValue())
          .collect(Collectors.joining("; "));
    }
    log.error("No cookies found in the request");
    return "";
  }
}

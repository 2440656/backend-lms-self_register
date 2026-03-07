package com.cognizant.lms.userservice.utils;

import org.springframework.stereotype.Component;

@Component
public class LogUtil {
  public static String getLogInfo(String processName, String status) {
    return String.format("%s : %s : ", processName, status);
  }

  public static String getLogError(String processName, String errorCode, String status) {
    return String.format("%s : %s : %s", processName, errorCode, status);
  }
}



package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.domain.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;


public class UserContext {

  public static String getCreatedBy() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    AuthUser authUser = (AuthUser) auth.getPrincipal();
    return authUser.getUsername();
  }

  public static String getModifiedBy() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    AuthUser authUser = (AuthUser) auth.getPrincipal();
    return authUser.getUsername();
  }

  public static String getUserEmail() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    AuthUser authUser = (AuthUser) auth.getPrincipal();
    return authUser.getUserEmail();
  }

  public static List<String> getUserRoles() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    AuthUser authUser = (AuthUser) auth.getPrincipal();
    return authUser.getUserRoles();
  }
}

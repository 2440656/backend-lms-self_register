package com.cognizant.lms.userservice.domain;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
public class AuthUser extends org.springframework.security.core.userdetails.User {

  private String userId;
  private String token;
  private String userEmail;
  private List<String> userRoles;
  private boolean isFirstLogin;
  private String viewOnlyAssignedCourses;


  public AuthUser(String username, String password,
                  Collection<? extends GrantedAuthority> authorities) {
    super(username, password, authorities);
  }

  public AuthUser(String username, String password, boolean enabled, boolean accountNonExpired,
                  boolean credentialsNonExpired, boolean accountNonLocked,
                  Collection<? extends GrantedAuthority> authorities) {
    super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
        authorities);
  }
}

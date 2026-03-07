package com.cognizant.lms.userservice.dto;

import com.cognizant.lms.userservice.domain.User;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSVProcessResponse {

  private List<User> validUsers;
  private Set<String> validEmail;
  private List<String> errors;
  private int successCount;
  private int failureCount;
  private int totalCount;

  public CSVProcessResponse(List<User> validUsers, List<String> errors, int successCount,
                            int failureCount, int totalCount) {
    this.validUsers = validUsers;
    this.errors = errors;
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.totalCount = totalCount;
  }

  public CSVProcessResponse(Set<String> validEmail, List<String> errors, int successCount,
                            int failureCount, int totalCount) {
    this.validEmail = validEmail;
    this.errors = errors;
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.totalCount = totalCount;
  }
}

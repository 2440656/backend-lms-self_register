package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryResponse {
  private Object data;
  private int status;
  private String error;
  private String lastEvaluatedKey;
  private int count;
}

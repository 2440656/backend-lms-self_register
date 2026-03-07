package com.cognizant.lms.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestConnectionRequestDto {
  private String organizationId;
  private String id;
  private String clientId;
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String clientSecret;
  private String clientCode;


}

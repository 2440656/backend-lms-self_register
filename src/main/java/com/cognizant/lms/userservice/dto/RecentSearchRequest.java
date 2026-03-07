package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecentSearchRequest {

  private String userId;
  private String type;
  private String searchQuery;
}
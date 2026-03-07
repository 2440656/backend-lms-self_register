package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantFeatureFlagsRequest {
  private Map<String, Boolean> featureFlags; // required: keys = feature flag names, values enable/disable
}


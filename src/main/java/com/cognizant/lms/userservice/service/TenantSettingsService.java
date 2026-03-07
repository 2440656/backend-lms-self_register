package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.dto.TenantSettingsRequest;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.dto.UpdateTenantFeatureFlagsRequest;

public interface TenantSettingsService {

  TenantSettingsResponse  getTenantSettingResponse (String settingName );

  String createTenantSettings( TenantSettingsRequest tenantSettingsRequest);

  String updateTenantSettings(String reviewEmail, String courseReviewCommentType);

  /**
   * Update tenant feature flags based on user's tenant code
   * @param request
   * @return
   */
  TenantFeatureFlagsDto updateTenantFeatureFlags(UpdateTenantFeatureFlagsRequest request);

  /**
   * Get tenant feature flags based on user's tenant code
   * @return
   */
  TenantFeatureFlagsDto getTenantFeatureFlags();

}

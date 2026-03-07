package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface TenantSettingsDao {
  TenantSettingsResponse getTenant(String settingName);
  TenantFeatureFlagsDto getTenantFeatureFlags(String tenantCode);

  boolean updateTenantSetings(Tenant tenant, String updatedBy, String updatedDate);

  boolean createTenantSettings(Tenant tenant);

  boolean updateTenantFeatureFlags(Map<String, Boolean> featureFlags, String tenantCode);
}
package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.CognitoConfigDTO;
import com.cognizant.lms.userservice.dto.TenantConfigDto;
import com.cognizant.lms.userservice.dto.TermsAndUseDTO;

public interface TeanatTableDao {
  CognitoConfigDTO fetchCognitoConfig(String prefix);

  TermsAndUseDTO getTermsAndUse(String tenantCode);

  TenantConfigDto fetchTenantConfig(String tenantCode);
}

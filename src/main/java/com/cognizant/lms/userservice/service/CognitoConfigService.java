package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.TeanatTableDao;
import com.cognizant.lms.userservice.dto.CognitoConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CognitoConfigService {
  private final Environment env;
  private final TeanatTableDao tenantTableDao;

  public CognitoConfigService(Environment env, TeanatTableDao tenantTableDao) {
    this.env = env;
    this.tenantTableDao = tenantTableDao;
  }

  public CognitoConfigDTO getCognitoDetails(String tenantPrefix) {
    log.info("Origin: {}", tenantPrefix);
    String prefix = tenantPrefix.toLowerCase();
    return tenantTableDao.fetchCognitoConfig(prefix);
  }
}

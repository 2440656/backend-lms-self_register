package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.TenantSettingsDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.dto.TenantSettingsRequest;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.dto.UpdateTenantFeatureFlagsRequest;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;


@Service
@Slf4j
public class TenantSettingsServiceImpl implements TenantSettingsService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile(Constants.EMAIL_PATTERN);
  private final TenantSettingsDao tenantSettingsDao;

  public TenantSettingsServiceImpl(TenantSettingsDao tenantSettingsDao) {
    this.tenantSettingsDao = tenantSettingsDao;
  }


  @Override
  public TenantSettingsResponse getTenantSettingResponse(String settingName) {
    TenantSettingsResponse tenantSettingsResponse = tenantSettingsDao.getTenant(settingName);
    return tenantSettingsResponse;
  }

  @Override
  public TenantFeatureFlagsDto getTenantFeatureFlags() {
    String tenantCode = TenantUtil.getTenantCode();
    try {
      if (!Constants.COGNIZANT_TENANT_CODE.equalsIgnoreCase(tenantCode)) {
        log.error("Feature flags API is only applicable for cognizant tenant. Current tenant: {}", tenantCode);
        throw new IllegalArgumentException("This feature is only available for cognizant tenant");
      }
      return tenantSettingsDao.getTenantFeatureFlags(tenantCode);
    } catch (Exception e) {
      log.error("Error retrieving tenant feature flags: {}", e.getMessage(), e);
      throw new RuntimeException("Error retrieving tenant feature flags: " + e.getMessage(), e);
    }
  }


  @Override
  public String createTenantSettings(TenantSettingsRequest tenantSettingsRequest) {
    try {
      if (!EMAIL_PATTERN.matcher(tenantSettingsRequest.getReviewEmail()).matches()) {
        return "Invalid Email format of the email, email must end with @[a-z,A-Z].com";
      }

      String tenantCode = TenantUtil.getTenantCode();
      String sortKey = tenantCode + Constants.HASH + Constants.TENANT_SETTING_NAME;
      String user = UserContext.getCreatedBy();
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
      String createOn = utcDateTime.format(formatter);

      Tenant tenantSetting = new Tenant();
      tenantSetting.setPk(tenantCode);
      tenantSetting.setSk(sortKey);
      tenantSetting.setReviewEmail(tenantSettingsRequest.getReviewEmail());
      tenantSetting.setCourseReviewCommentType(tenantSettingsRequest.getCourseReviewCommentType());
      tenantSetting.setCreatedBy(user);
      tenantSetting.setCreatedOn(createOn);
      tenantSetting.setSettingName(Constants.TENANT_SETTING_NAME);
      tenantSetting.setType(Constants.SETTING);

      return tenantSettingsDao.createTenantSettings(tenantSetting)
          ? "Tenant Settings Created Successfully."
          : "Error creating tenant settings.";
    } catch (Exception e) {
      log.error("Error occurred while creating tenant settings: {}", e.getMessage(), e);
     throw new RuntimeException("Error creating tenant settings: " + e.getMessage(), e);
    }
  }




  @Override
  public String updateTenantSettings(String reviewEmail, String courseReviewCommentType) {
    try {
      if (!EMAIL_PATTERN.matcher(reviewEmail).matches()) {
        return "Invalid Email format of the email, email must end with @[a-z,A-Z].com";
      }

      String tenantCode = TenantUtil.getTenantCode();
      String sortKey = tenantCode + Constants.HASH + Constants.TENANT_SETTING_NAME;
      String updatedBy = UserContext.getCreatedBy();
      ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT);
      String updatedOn = utcDateTime.format(formatter);

      Tenant tenantSetting = new Tenant();
      tenantSetting.setPk(tenantCode);
      tenantSetting.setSk(sortKey);
      tenantSetting.setReviewEmail(reviewEmail);
      tenantSetting.setCourseReviewCommentType(courseReviewCommentType);
      tenantSetting.setSettingName(Constants.TENANT_SETTING_NAME);
      tenantSetting.setType(Constants.SETTING);

      return tenantSettingsDao.updateTenantSetings(tenantSetting, updatedBy, updatedOn)
          ? "Tenant settings updated successfully."
          : "Failed to update tenant settings.";
    } catch (Exception e) {
      log.error("Error occurred while updating tenant settings: {}", e.getMessage(), e);
      throw new RuntimeException("Error updating tenant settings: " + e.getMessage(), e);
    }
  }

  @Override
  public TenantFeatureFlagsDto updateTenantFeatureFlags(UpdateTenantFeatureFlagsRequest request) {
    String tenantCode = TenantUtil.getTenantCode();
    if (!Constants.COGNIZANT_TENANT_CODE.equalsIgnoreCase(tenantCode)) {
        log.error("Feature flags API is only applicable for cognizant tenant. Current tenant: {}", tenantCode);
        throw new IllegalArgumentException("This feature is only available for cognizant tenant");
    }
    try {
      if (request == null || request.getFeatureFlags() == null) {
        log.warn("Empty feature flags update request - no changes applied");
        return tenantSettingsDao.getTenantFeatureFlags(tenantCode);
      }
      boolean updated = tenantSettingsDao.updateTenantFeatureFlags(request.getFeatureFlags(), tenantCode);
      if (!updated) {
        log.warn("Failed to update tenant feature flags in DB");
      }
      return tenantSettingsDao.getTenantFeatureFlags(tenantCode);
    } catch (Exception e) {
      log.error("Error updating tenant feature flags: {}", e.getMessage(), e);
      throw new RuntimeException("Error updating tenant feature flags: " + e.getMessage(), e);
    }
  }
}

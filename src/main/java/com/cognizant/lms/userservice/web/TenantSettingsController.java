package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TenantFeatureFlagsDto;
import com.cognizant.lms.userservice.dto.TenantSettingsRequest;
import com.cognizant.lms.userservice.dto.TenantSettingsResponse;
import com.cognizant.lms.userservice.dto.UpdateTenantFeatureFlagsRequest;
import com.cognizant.lms.userservice.service.TenantSettingsService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("api/v1/tenantSettings")
@Slf4j
public class TenantSettingsController {

  private final TenantSettingsService tenantSettingsService;

  public TenantSettingsController(TenantSettingsService tenantSettingsService) {
    this.tenantSettingsService = tenantSettingsService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','catalog-admin','learner')")
  public ResponseEntity<HttpResponse> getTenantSetting (
      @RequestParam(value = "settingName", required = false, defaultValue = "content-moderation") String settingName ){

      TenantSettingsResponse tenantSettingsResponse = tenantSettingsService.getTenantSettingResponse(settingName);

      if (tenantSettingsResponse == null) {
        HttpResponse response = new HttpResponse();
        response.setData(null);
        response.setStatus(204);
        response.setError("Tenant setting not found for name: " + settingName);
        return ResponseEntity.status(HttpStatus.OK).body(response);
      }
      HttpResponse response = new HttpResponse();
      response.setData(tenantSettingsResponse);
      response.setStatus(200);
      response.setError(null);
      return ResponseEntity.ok(response);

  }

  @PostMapping("/contentModeration")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','catalog-admin')")
  public ResponseEntity<HttpResponse> createTenantSettings(
      @RequestBody TenantSettingsRequest tenantSettingsRequest) {
    HttpResponse response = new HttpResponse();
    tenantSettingsService.createTenantSettings(tenantSettingsRequest);
    response.setStatus(HttpStatus.CREATED.value());
    response.setData("Tenant Settings Created Successfully.");
    return ResponseEntity.status(response.getStatus()).body(response);
  }
  @PutMapping("/contentModeration")
  @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','catalog-admin')")
  public ResponseEntity<HttpResponse> updateTenantSettings(
      @RequestParam(value = "reviewEmail") String reviewEmail,
      @RequestParam(value = "courseReviewCommentType") String courseReviewCommentType){

      HttpResponse response = new HttpResponse();
      tenantSettingsService.updateTenantSettings(reviewEmail,courseReviewCommentType);
      response.setStatus(HttpStatus.OK.value());
      response.setData("Tenant Updated Successfully.");
      return ResponseEntity.status(response.getStatus()).body(response);
    }

  /**
   * Get Tenant Feature Flags, as per requirement only system-admin can access this API
   * @return
   */
  @GetMapping("featureFlags")
  @PreAuthorize("hasAnyRole('system-admin','catalog-admin','learner','content-author','mentor')")
  public ResponseEntity<HttpResponse> getTenantFeatureFlags (){
    try {
      TenantFeatureFlagsDto tenantFeatureFlagsDto = tenantSettingsService.getTenantFeatureFlags();
      if (tenantFeatureFlagsDto == null) {
        HttpResponse response = new HttpResponse();
        response.setData(null);
        response.setStatus(404);
        response.setError("Tenant Feature flags not found");
        return ResponseEntity.status(HttpStatus.OK).body(response);
      }
      HttpResponse response = new HttpResponse();
      response.setData(tenantFeatureFlagsDto);
      response.setStatus(200);
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Unexpected error: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(null);
    }
  }

  /**
   * Update Tenant Feature Flags, as per requirement only system-admin can access this API
   * @param request
   * @return
   */
  @PostMapping("featureFlags")
  @PreAuthorize("hasAnyRole('system-admin')")
  public ResponseEntity<HttpResponse> updateTenantFeatureFlags(@RequestBody UpdateTenantFeatureFlagsRequest request) {
    try {
      TenantFeatureFlagsDto tenantFeatureFlagsDto = tenantSettingsService.updateTenantFeatureFlags(request);
      if (tenantFeatureFlagsDto == null) {
        HttpResponse response = new HttpResponse();
        response.setData(null);
        response.setStatus(404);
        response.setError("Tenant Feature flags record not found for update");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }
      HttpResponse response = new HttpResponse();
      response.setData(tenantFeatureFlagsDto);
      response.setStatus(200);
      response.setError(null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Unexpected error: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(null);
    }
  }
}

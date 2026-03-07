package com.cognizant.lms.userservice.client;

import com.cognizant.lms.userservice.config.RestTemplateConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.TokenUtil;
import com.cognizant.lms.userservice.utils.SanitizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class CourseManagementServiceClient {
  private final RestTemplate restTemplate;
  private final String courseManagementServiceUrl;
  private final TokenUtil tokenUtil;

  @Autowired
  public CourseManagementServiceClient(RestTemplateConfig restTemplateConfig,
                                       @Value("${COURSE_MANAGEMENT_SERVICE_API_URL}") String courseManagementServiceUrl ,TokenUtil tokenUtil) {
    this.restTemplate = restTemplateConfig.restTemplate();
    this.courseManagementServiceUrl = courseManagementServiceUrl;
    this.tokenUtil = tokenUtil;
  }

  public String updateCourseCreatedByName(String emailId, String createdByName) {
    // Sanitize inputs to prevent SSRF
    String safeEmailId = SanitizeUtil.sanitizeOriginalData(emailId);
    String safeCreatedByName = SanitizeUtil.sanitizeOriginalData(createdByName);
    String url = courseManagementServiceUrl + "/api/v1/courses/updateCreatedBy";
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
        .queryParam("emailId", safeEmailId)
        .queryParam("createdBy", safeCreatedByName);

    // Prepare headers
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, tokenUtil.getAuthorizationHeader());
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(Constants.TENANT_HEADER, TenantUtil.getTenantDetails().getTenantIdentifier());
    HttpEntity<?> entity = new HttpEntity<>(headers);

    log.info("Updating course created by name for emailId: {}", safeEmailId);

    ResponseEntity<Object> responseEntity = restTemplate.exchange(
        builder.toUriString(),
        HttpMethod.POST,
        entity,
        new ParameterizedTypeReference<>() {}
    );
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      return Objects.requireNonNull(responseEntity.getBody()).toString();
    } else {
      log.error("Failed to update course created by name for emailId: {}", safeEmailId);
      return null;
    }
  }


  public HttpStatusCode testConnection(String id, String clientId, String clientCode){
    if (id == null || !id.matches("^[A-Za-z0-9_-]{1,64}$")) {
      log.warn("Blocked invalid id");
      return HttpStatus.BAD_REQUEST;
    }
    String safeId = SanitizeUtil.sanitizeOriginalData(id);
    String url = UriComponentsBuilder
        .fromHttpUrl("https://cognizant-sandbox.udemy.com/api-2.0")
        .pathSegment("organizations", safeId, "courses", "list")
        .toUriString();
    try{
      String key = clientId + ":" + clientCode;
      String masKey = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));

      // Set headers for Basic Auth
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Basic " + masKey);


      // Create HTTP entity
      HttpEntity<String> entity = new HttpEntity<>(headers);

      // Make the API call
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("Test Connection Failed: ");
      }
      return response.getStatusCode();

    }
    catch(Exception e){
      log.error("Error during test connection");
      return HttpStatus.INTERNAL_SERVER_ERROR;

    }
    // Encode clientId:clientSecret in Base64

  }

}
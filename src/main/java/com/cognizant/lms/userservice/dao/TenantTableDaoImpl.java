package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.CognitoConfigDTO;
import com.cognizant.lms.userservice.dto.LanguageDto;
import com.cognizant.lms.userservice.dto.TenantConfigDto;
import com.cognizant.lms.userservice.dto.TermsAndUseDTO;
import com.cognizant.lms.userservice.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TenantTableDaoImpl implements TeanatTableDao {
  private final DynamoDbClient dynamoDbClient;
  private final String tableName;

  private final String hostRootDomainCsv;
  private final String uiPrefixesCsv;

  public TenantTableDaoImpl(DynamoDBConfig dynamoDBConfig,
                        @Value("${AWS_DYNAMODB_TENANT_CONFIG_TABLE_NAME}") String tableName,
                            @Value("${ALLOWED_DOMAINS}") String hostRootDomainCsv,
                            @Value("${UI_PREFIXES}") String uiPrefixesCsv){
    dynamoDbClient = dynamoDBConfig.dynamoDbClient();
    this.tableName = tableName;
    this.hostRootDomainCsv = hostRootDomainCsv;
    this.uiPrefixesCsv = uiPrefixesCsv;
  }

  @Override
  public CognitoConfigDTO fetchCognitoConfig(String origin) {
    if (origin == null) {
      log.error("fetchCognitoConfig called with null origin");
      throw new RuntimeException("Origin is null");
    }

    String tenantIdentifier = getTenantIdentifier(origin);
    log.info("Fetching Cognito Config; origin={}, resolvedTenantIdentifier={}", origin, tenantIdentifier);

    try {
      CognitoConfigDTO cognitoConfigDTO = new CognitoConfigDTO();
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .indexName("gsi_sort_createdOn")
          .keyConditionExpression("tenantIdentifier  = :tenantIdentifier")
          .scanIndexForward(true)
          .filterExpression("#type = :type")
          .expressionAttributeNames(Map.of("#type", "type"))
          .expressionAttributeValues(
              Map.of(
                  // NOTE: still using origin as per existing behavior
                  ":tenantIdentifier", AttributeValue.builder().s(tenantIdentifier).build(),
                  ":type", AttributeValue.builder().s("tenant").build()
              ))
          .build();

      log.debug("DynamoDB Query (CognitoConfig): table={}, index={}, keyExpr={}, filterExpr={}",
          tableName, "gsi_sort_createdOn", "tenantIdentifier  = :tenantIdentifier", "#type = :type");

      QueryResponse queryResult = dynamoDbClient.query(queryRequest);
      int count = queryResult.hasItems() ? queryResult.items().size() : 0;
      log.info("CognitoConfig query returned {} item(s) for tenantIdentifier={} (origin={})", count, tenantIdentifier, origin);

      if (!queryResult.hasItems() || queryResult.items().isEmpty()) {
        log.warn("No Cognito Config found for tenantIdentifier={}, origin={}", tenantIdentifier, origin);
        return cognitoConfigDTO; // empty DTO
      }

      Map<String, AttributeValue> item = queryResult.items().get(0);
      cognitoConfigDTO.setClientId(getValueOrNull(item, "appClientId"));
      cognitoConfigDTO.setIssuer(getValueOrNull(item, "awsCognitoIssuer"));
      cognitoConfigDTO.setCertUrl(getValueOrNull(item, "awsCertUrl"));
      cognitoConfigDTO.setTenantCode(getValueOrNull(item, "pk"));
      log.info("Fetched Cognito Config for origin {}: {}", origin, cognitoConfigDTO);
      return cognitoConfigDTO;
    } catch (DynamoDbException dbe) {
      log.error("DynamoDB error while fetching Cognito Config for origin {}: {}", origin, dbe.getMessage(), dbe);
      throw dbe;
    } catch (Exception e) {
      log.error("Unexpected error while fetching Cognito Config for origin {}: {}", origin, e.getMessage(), e);
      throw e;
    }
  }


    public String getTenantIdentifier(String origin) {
      List<String> allowedUrls = splitCsvLowerTrim(hostRootDomainCsv);
      if (allowedUrls.isEmpty()) {
        throw new IllegalStateException("hostRootDomain is empty");
      }

      String defaultUrl = allowedUrls.getFirst();
      Set<String> prefixes = new HashSet<>(splitCsvLowerTrim(uiPrefixesCsv));

      String host = extractHost(origin);
      if (isBlank(host)) {
        throw new IllegalArgumentException("Origin header missing or invalid");
      }

      // Validate allowed base:
      // isAllowed = host == DEFAULT_URL || host endsWith .DEFAULT_URL || host == any allowedUrl
      boolean isAllowed =
              host.equals(defaultUrl) ||
                      host.endsWith("." + defaultUrl) ||
                      allowedUrls.contains(host);

      if (!isAllowed) {
        throw new IllegalArgumentException(
                "Origin '" + host + "' is not allowed (must end with '" + defaultUrl + "')"
        );
      }

      // If host is exactly one of the allowed root domains but not the default,
      // safest behavior is to treat it as itself (since your Node allows exact matches via allowedUrls.some).
      if (allowedUrls.contains(host) && !host.endsWith("." + defaultUrl) && !host.equals(defaultUrl)) {
        return host;
      }

      // Base domain maps to itself
      if (host.equals(defaultUrl)) return defaultUrl;

      // --- Normalize learner-catalog before further parsing ---
      // Supports both: learner-catalog.<tenant>... and learner-catalog-<tenant>...
      if (host.startsWith("learner-catalog." + defaultUrl)) {
        // prefix-only -> base
        return defaultUrl;
      }
      if (host.startsWith("learner-catalog-")) {
        host = host.replaceFirst("^learner-catalog-", "");
      }

      // Left side before ".<defaultUrl>"
      String suffix = "." + defaultUrl;
      if (!host.endsWith(suffix)) {
        // Should not happen because of validation, but keep safe.
        return host;
      }
      String left = host.substring(0, host.length() - suffix.length());

      // Node splits by "-"
      String[] parts = left.split("-");

      // Case: "<prefix>.<root>" -> base
      if (parts.length == 1 && prefixes.contains(parts[0])) {
        return defaultUrl;
      }

      // Case: "<prefix>-<tenant>.<root>" -> "<tenant>.<root>"
      if (parts.length >= 2 && prefixes.contains(parts[0])) {
        String tenant = String.join("-", Arrays.copyOfRange(parts, 1, parts.length));
        return tenant + "." + defaultUrl;
      }

      // Case: "<tenant>.<root>" -> itself
      return host;
    }

    // -------------------- helpers --------------------

    private static String extractHost(String origin) {
      if (origin == null) return null;

      // Node: origin = origin.split(",")[0]
      String first = origin.split(",")[0].trim();
      if (first.isEmpty()) return null;

      try {
        if (first.matches("(?i)^https?://.*")) {
          URI uri = URI.create(first);
          String h = uri.getHost();
          return h == null ? null : h.toLowerCase(Locale.ROOT);
        } else {
          // Strip optional scheme, then strip port/path
          String normalized = first
                  .replaceFirst("(?i)^\\s*https?://?", "")
                  .replaceFirst("[:/].*$", "");
          return normalized.toLowerCase(Locale.ROOT);
        }
      } catch (Exception e) {
        // Fallback similar to Node catch
        String normalized = first
                .replaceFirst("(?i)^\\s*https?://?", "")
                .replaceFirst("[:/].*$", "");
        return normalized.toLowerCase(Locale.ROOT);
      }
    }

    private static List<String> splitCsvLowerTrim(String csv) {
      if (csv == null) return List.of();
      String[] parts = csv.toLowerCase(Locale.ROOT).split(",");
      List<String> out = new ArrayList<>();
      for (String p : parts) {
        String t = p.trim();
        if (!t.isEmpty()) out.add(t);
      }
      return out;
    }


    private static boolean isBlank(String s) {
      return s == null || s.trim().isEmpty();
    }

  @Override
  public TermsAndUseDTO getTermsAndUse(String tenantCode) {
    log.info("Fetching Terms and Use for tenant: {}", tenantCode);
    try {
      Map<String, AttributeValue> key = new HashMap<>();
      key.put("pk", AttributeValue.builder().s(tenantCode).build());
      key.put("sk", AttributeValue.builder().s(tenantCode + Constants.HASH + "TERMS_AND_USE").build());

      GetItemRequest getItemRequest = GetItemRequest.builder()
              .tableName(tableName)
              .key(key)
              .build();

      GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
      TermsAndUseDTO termsAndUseDTO = new TermsAndUseDTO();
      termsAndUseDTO.setTenantCode(tenantCode);

      if (getItemResponse.hasItem()) {
        log.info("Successfully fetched Terms and Use for tenant: {}", tenantCode);
        var item = getItemResponse.item();
        var enableAttr = item.get("enableTermsAndUse");
        termsAndUseDTO.setEnableTermsAndUse(enableAttr != null && enableAttr.bool() != null && enableAttr.bool());
        termsAndUseDTO.setTermsAndUseContent(getValueOrNull(item, "termsAndUseContent"));
        termsAndUseDTO.setCreatedOn(getValueOrNull(item, "createdOn"));
        return termsAndUseDTO;
      } else {
        log.warn("Terms and Use not found for tenant: {}", tenantCode);

        termsAndUseDTO.setTermsAndUseContent("");
        return termsAndUseDTO;
      }
    } catch (Exception e) {
      log.error("Error fetching Terms and Use for tenant {}: {}", tenantCode, e.getMessage());
      return new TermsAndUseDTO();
    }
  }

  @Override
  public TenantConfigDto fetchTenantConfig(String tenantCode) {
    log.info("Fetching Tenant Config for tenant code: {}", tenantCode);
    try {
      TenantConfigDto tenantConfigDto = new TenantConfigDto();
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression("pk  = :pk and sk = :sk")
          .scanIndexForward(true)
          .expressionAttributeValues(
              Map.of(
                  ":pk", AttributeValue.builder().s(tenantCode).build(),
                  ":sk", AttributeValue.builder().s(tenantCode + "#config").build()
              ))
          .build();

      log.debug("DynamoDB Query (TenantConfig): table={}, keyExpr={}", tableName, "pk  = :pk and sk = :sk");

      QueryResponse queryResult = dynamoDbClient.query(queryRequest);
      int count = queryResult.hasItems() ? queryResult.items().size() : 0;
      log.info("TenantConfig query returned {} item(s) for tenantCode={}", count, tenantCode);

      if (!queryResult.hasItems() || queryResult.items().isEmpty()) {
        log.warn("Tenant Config not found for tenantCode={}", tenantCode);
        return tenantConfigDto; // empty DTO
      }

      Map<String, AttributeValue> item = queryResult.items().get(0);
      tenantConfigDto.setAdminURL(getValueOrNull(item, "adminURL"));
      tenantConfigDto.setAppClientId(getValueOrNull(item, "appClientId"));
      tenantConfigDto.setAwsCognitoIssuer(getValueOrNull(item, "awsCognitoIssuer"));
      tenantConfigDto.setAwsCertUrl(getValueOrNull(item, "awsCertUrl"));
      tenantConfigDto.setCookieDomain(getValueOrNull(item, "cookieDomain"));
      tenantConfigDto.setHost(getValueOrNull(item, "host"));
      tenantConfigDto.setHostedUiDomain(getValueOrNull(item, "hostedUiDomain"));
      tenantConfigDto.setHostedUiLoginUrl(getValueOrNull(item, "hostedUiLoginUrl"));
      tenantConfigDto.setLearnerCatalogURL(getValueOrNull(item, "learnerCatalogURL"));
      tenantConfigDto.setLearnerURL(getValueOrNull(item, "learnerURL"));
      tenantConfigDto.setMainURL(getValueOrNull(item, "mainURL"));
      tenantConfigDto.setName(getValueOrNull(item, "name"));
      tenantConfigDto.setUserPoolAppId(getValueOrNull(item, "userPoolAppId"));
      tenantConfigDto.setUserPoolAppSecret(getValueOrNull(item, "userPoolAppSecret"));
      tenantConfigDto.setUserPoolDomain(getValueOrNull(item, "userPoolDomain"));
      tenantConfigDto.setUserPoolId(getValueOrNull(item, "userPoolId"));
      tenantConfigDto.setMediaURL(getValueOrNull(item, "mediaURL"));
      tenantConfigDto.setUserMediaURL(getValueOrNull(item, "userMediaURL"));
      tenantConfigDto.setLogoutURL(getValueOrNull(item, "logoutURL"));
      tenantConfigDto.setSiteLogoPath(getValueOrNull(item, "siteLogoPath"));

      // Set betaEnabled from DynamoDB item if present
      AttributeValue betaEnabledAttr = item.get("betaEnabled");
      if (betaEnabledAttr != null && betaEnabledAttr.bool() != null) {
        tenantConfigDto.setBetaEnabled(betaEnabledAttr.bool());
      } else {
        tenantConfigDto.setBetaEnabled(false); // default to false if not present
      }

      String languageCodes = getValueOrNull(item, "language");
      List<LanguageDto> languages = Optional.ofNullable(languageCodes)
          .filter(codes -> !codes.isBlank())
          .map(codes -> codes.split(","))
          .stream()
          .flatMap(Arrays::stream)
          .map(String::trim)
          .filter(code -> !code.isEmpty())
          .map(Language::fromCode)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(lang -> new LanguageDto(lang.getLang_code(), lang.getLanguage()))
          .collect(Collectors.toList());
      tenantConfigDto.setLanguage(languages);
      tenantConfigDto.setTenantServiceURL(getValueOrNull(item, "tenantServiceURL"));
      log.info("Fetched Tenant Config for tenant code {}: {}", tenantCode, tenantConfigDto);
      return tenantConfigDto;
    } catch (DynamoDbException dbe) {
      log.error("DynamoDB error while fetching Tenant Config for tenantCode {}: {}", tenantCode, dbe.getMessage(), dbe);
      throw dbe;
    } catch (Exception e) {
      log.error("Unexpected error while fetching Tenant Config for tenantCode {}: {}", tenantCode, e.getMessage(), e);
      throw e;
    }
  }

  private String getValueOrNull(Map<String, AttributeValue> item, String key) {
    return item.get(key) != null ? item.get(key).s() : null;
  }
}

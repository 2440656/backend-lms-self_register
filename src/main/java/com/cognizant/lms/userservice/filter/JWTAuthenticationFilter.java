package com.cognizant.lms.userservice.filter;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.CognitoConfigDTO;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.service.CognitoConfigService;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.CustomSigningKeyResolver;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.TokenType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


@Slf4j
public class JWTAuthenticationFilter extends BasicAuthenticationFilter {
  private String awsCognitoClientId;
  private String awsCognitoIssuer;
  private String awsCognitoCertUrl;
  private final UserService userService;
  private RoleDao roleDao;

  private CognitoConfigService cognitoConfigService;

  private UserFilterSortDao userFilterSortDao;

  public JWTAuthenticationFilter(AuthenticationManager authenticationManager,
                                 @Value("${AWS_COGNITO_CERT_URL}") String awsCognitoCertUrl,
                                 @Value("${AWS_COGNITO_CLIENT_ID}") String awsCognitoClientId,
                                 @Value("${AWS_COGNITO_ISSUER}") String awsCognitoIssuer,
                                 RoleDao roleDao, UserFilterSortDao userFilterSortDao, UserService userService,
                                 CognitoConfigService cognitoConfigService) {
    super(authenticationManager);
    this.awsCognitoCertUrl = awsCognitoCertUrl;
    this.userService = userService;
    this.awsCognitoClientId = awsCognitoClientId;
    this.awsCognitoIssuer = awsCognitoIssuer;
    this.roleDao = roleDao;
    this.userFilterSortDao = userFilterSortDao;
    this.cognitoConfigService = cognitoConfigService;
  }

  CognitoConfigDTO cognitoConfigDTO;

  @SuppressWarnings("checkstyle:LocalVariableName")
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    String[] publicURL = {"/actuator/health", "/api/v1/auth/register/email",
      "/api/v1/auth/register/email/verify-otp", "/api/v1/auth/register/email/resend-otp",
      "/api/v1/auth/email/verify", "/api/v1/auth/email/resend"};
    String requestPath = request.getRequestURI();
    String requestUrl = request.getRequestURL() != null ? request.getRequestURL().toString() : "";
    if ((requestPath != null && Arrays.stream(publicURL)
        .anyMatch(url -> requestPath.equalsIgnoreCase(url)
          || requestPath.toLowerCase().contains(url.toLowerCase())))
      || Arrays.stream(publicURL).anyMatch(url -> requestUrl.toLowerCase().contains(url.toLowerCase()))) {
      filterChain.doFilter(request, response);
      return;
    }

    String origin = request.getHeader("Origin");
    String tenantIdentifier;
    if( origin == null || origin.isEmpty()) {
      tenantIdentifier = request.getHeader(Constants.TENANT_HEADER);
    }
    else{
      tenantIdentifier  = getTenantIdentifier(origin);
    }
    cognitoConfigDTO = cognitoConfigService.getCognitoDetails(tenantIdentifier);
    log.info("Cognito Config DTO in JWTAuthenticationFilter: " + cognitoConfigDTO);
    response.setHeader("X-FRAME-OPTIONS", "DENY"); //sast12Apr
    log.info("Request URL: " + request.getRequestURL().toString());
    if (requestPath!=null && requestPath.contains(Constants.MLS_ENROLL_ENDPOINT)){
      String token = sanitizeData(getTokenFromAuthorizationHeader(request));
      if (token == null) {
        createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), "No Token Found", response);
        return;
      }
      // Basic JWT format validation
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", response);
        return;
      }
      try {
        int index = token.lastIndexOf('.');
        String tokenWithOutSignature = token.substring(0, index + 1);
        Claims claimsBody = Jwts.parserBuilder().build()
                .parseClaimsJwt(tokenWithOutSignature).getBody();
        String tenantCode = request.getHeader("X-Tenant-Code");
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk(tenantCode);
        TenantUtil.setTenantDetails(tenantDTO);

        // Expiration validation
        if (claimsBody.getExpiration() != null && claimsBody.getExpiration().before(new java.util.Date())) {
          createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), "JWT token expired", response);
          return;
        }
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SERVICE"));
        AuthUser principal = new AuthUser("service-account", "", authorities);
        principal.setToken(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
        return;
      } catch (Exception e) {
        log.error("Service token validation failed: {}", e.getMessage());
        createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid service token", response);
        return;
      }

    }

    if (Arrays.stream(publicURL)
            .anyMatch(url -> request.getRequestURL().toString().contains(url))) {
      filterChain.doFilter(request, response);
    } else {
      String token = sanitizeData(request.getHeader(Constants.AUTHORIZATION));

      if (token != null && !token.isEmpty()) {
        if (token.startsWith("Bearer ")) {
          token = token.substring(7);
        }
        try {
          SigningKeyResolver signingKeyResolver = getMySigningKeyResolver();
          int index = token.lastIndexOf('.');
          String tokenWithOutSignature = token.substring(0, index + 1);
          String algo =
                  Jwts.parserBuilder().build().parseClaimsJwt(tokenWithOutSignature)
                          .getHeader().get("alg").toString();
          if (algo.equals("RS256")) {
            String tokenType =
                    Jwts.parserBuilder().build()
                            .parseClaimsJwt(tokenWithOutSignature).getBody().get("token_use")
                            .toString();
            String claimName = tokenType.equals(TokenType.id.name()) ? "aud" :
                    tokenType.equals(TokenType.access.name()) ? "client_id" : null;
            Jws<Claims>
                    claims =
                    Jwts.parserBuilder().require(claimName, cognitoConfigDTO.getClientId())
                            .requireIssuer(cognitoConfigDTO.getIssuer())
                            .setSigningKeyResolver(signingKeyResolver).build()
                            .parseClaimsJws(token);

            String sub = claims.getBody().getSubject();

            String providerName = getProviderName(claims);
            String userEmail = claims.getBody().get("email", String.class);
            String firstName = claims.getBody().get("given_name", String.class);
            String lastName = claims.getBody().get("family_name", String.class);
            Map<String,String> customClaims = claims.getBody().get("custom_claim", Map.class);

            if(userEmail == null || userEmail.trim().isEmpty()) {
              if(providerName != null && providerName.equalsIgnoreCase(Constants.SOCIAL_IDP_GITHUB)){
                log.error("Github account is private, please make it public");
                createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "Github account is private, please make it public", response);
                return;
              }
              log.error("User email is missing");
              createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                      "User email is missing", response);
              return;
            }

            if(customClaims == null || customClaims.isEmpty()) {
              log.error("Custom claims are missing in the token");
              createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                      "Custom claims are missing in the token", response);
              return;
            }

            TenantDTO tenant = new TenantDTO();
            String portal = request.getHeader("X-Portal-Id");
            log.info("Portal ID from request header: " + portal);
            tenant.setPk(customClaims.get("tenantCode"));
            tenant.setIdpPreferences(customClaims.get("idpPreferences"));
            tenant.setTenantIdentifier(request.getHeader(Constants.TENANT_HEADER));
            tenant.setClientId(cognitoConfigDTO.getClientId());
            tenant.setIssuer(cognitoConfigDTO.getIssuer());
            tenant.setCertUrl(cognitoConfigDTO.getCertUrl());
            tenant.setPortal(portal);

            if (tenant.getPk() == null || tenant.getPk().isEmpty()) {
              log.error("Tenant not found in the system");
              createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                      "Tenant not found in the system", response);
              return;
            }

            TenantUtil.setTenantDetails(tenant);

            log.info("Tenant details set in TenantUtil: " + TenantUtil.getTenantDetails());
            log.info("customClaims status: " + customClaims.get("status"));
            log.info("providerName: " + providerName);

            if(customClaims.get("status").equalsIgnoreCase(Constants.NO_USER)  && !Constants.LOGIN_OPTION_COGNIZANT_SSO.equalsIgnoreCase(providerName))
            {
              String message = "You are not authorized to access Skillspring.";
              log.info(message);
              createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), message, response);
              return;
            }
            userEmail= userEmail.toLowerCase();

            log.info("Custom Claims: " + customClaims);
            User user = userFromClaims(customClaims, tenant);

            log.info("User fetched from claims: " + user.toString());

            User loggedInUser = user.getStatus().equalsIgnoreCase(Constants.ACTIVE_STATUS)? user: null;

            if (loggedInUser == null) {
              User inactiveUser = user.getStatus().equalsIgnoreCase(Constants.IN_ACTIVE_STATUS)? user: null;
              String inActiveMessage = "Your account has been deactivated. Please reach out to the Admin for assistance.";
              if(!Constants.LOGIN_OPTION_COGNIZANT_SSO.equalsIgnoreCase(providerName)){
                String message = inactiveUser == null ?
                        "You are not authorized to access Skillspring." : inActiveMessage;
                log.info(message);
                createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), message, response);
                return;
              }
              if (inactiveUser != null){
                log.info(inActiveMessage);
                createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), inActiveMessage, response);
                return;
              }
              else{
                log.info("Creating Cognizant SSO user with Learner role");
                if(request.getRequestURL().toString().toLowerCase().contains(Constants.REGISTER_LOGIN_API)) {
                  loggedInUser = userService.createUserForCognizantSSO(firstName, lastName, userEmail);
                }
              }
            }
            else{
              String loginOption = loggedInUser.getLoginOption() == null || loggedInUser.getLoginOption().isEmpty() ?
                      Constants.LOGIN_OPTION_LMS_CREDENTIALS : loggedInUser.getLoginOption();
              if ((providerName == null || providerName.isEmpty()) &&
                      !loginOption.equalsIgnoreCase(Constants.LOGIN_OPTION_LMS_CREDENTIALS)) {
                createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "Invalid login option, Please login with " + loginOption, response);
                return;
              } else if (providerName != null && (providerName.equalsIgnoreCase(Constants.SOCIAL_IDP_GITHUB) || providerName.equalsIgnoreCase(Constants.SOCIAL_IDP_GOOGLE)) &&
                      !loginOption.equalsIgnoreCase(Constants.LOGIN_OPTION_SOCIAL_IDP)) {
                createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "Invalid login option, Please login with " + loginOption, response);
                return;
              } else if (providerName != null && providerName.equalsIgnoreCase(Constants.LOGIN_OPTION_COGNIZANT_SSO) &&
                      !loginOption.equalsIgnoreCase(Constants.LOGIN_OPTION_COGNIZANT_SSO)) {
                createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "Invalid login option, Please login with " + loginOption, response);
                return;
              }
            }

            String userRoles = loggedInUser.getRole();
            List<String> roleList =
                    userRoles.contains(", ") ? List.of(userRoles.split(", ")) :
                            userRoles.contains(",") ? List.of(userRoles.split(",")) :
                                    List.of(userRoles);
            String userName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            roleList.forEach(role
                    -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            AuthUser principal = new AuthUser(userName, "", authorities);
            principal.setUserId(customClaims.get("pk"));
            principal.setToken(token);
            principal.setUserEmail(userEmail);
            principal.setUserRoles(roleList);
            principal.setViewOnlyAssignedCourses(loggedInUser.getViewOnlyAssignedCourses());
            if (loggedInUser.getLastLoginTimestamp() == null
                    || loggedInUser.getLastLoginTimestamp().isEmpty()) {
              principal.setFirstLogin(
                      userFilterSortDao.updateFirstLoggedInUser(loggedInUser, userName));
            } else {
              principal.setFirstLogin(false);
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            Set<String> rolesFromDB =
                    roleDao.getRoles().stream().map(RoleDto::getSk).collect(
                            Collectors.toSet());
            if (roleList.stream().anyMatch(rolesFromDB::contains)) {
              log.info("User is authorized to access the resource");
              filterChain.doFilter(request, response);
            } else {
              createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                      "User is not authorized to access the resource", response);
            }
          }
          else {
            createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                    "token is not signed using RS256 algorithm", response);
          }
        }
        catch (JwtException e) {
          log.info(e.getLocalizedMessage());
          createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), response);
        }

        catch (Exception e) {
          log.error("Error occurred while parsing JWT token: {}", e.getMessage());
          createTokenErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong. Please try again later.", response);
        }
      }
      else {
        log.info("NO Token Found");
        createTokenErrorResponse(HttpStatus.UNAUTHORIZED.value(), "No Token Found", response);
      }
    }
  }

  private String getTenantIdentifier(String url) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("URL cannot be null or empty");
    }
    return url.toLowerCase().replaceFirst("https?://", "");
  }

  private String getTokenFromAuthorizationHeader(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }



  private SigningKeyResolver getMySigningKeyResolver() {
    return new CustomSigningKeyResolver(cognitoConfigDTO.getCertUrl());
  }

  private static String getProviderName(Jws<Claims> claims) {
    List<Map<String, Object>> identities = claims.getBody().get("identities", List.class);

    if (identities == null || identities.isEmpty()) {
      return null;
    }

    for (Map<String, Object> identity : identities) {
      if (identity.containsKey("providerName")) {
        return identity.get("providerName").toString();
      }
    }
    return null;
  }


  private void createTokenErrorResponse(int code, String message, HttpServletResponse res)
          throws IOException {
    res.setStatus(HttpStatus.UNAUTHORIZED.value());
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    HttpResponse httpResponse = new HttpResponse();
    httpResponse.setError(message);
    ObjectMapper objectMapper = new ObjectMapper();
    res.getWriter().print(objectMapper.writeValueAsString(httpResponse));
  }

  private User userFromClaims(Map<String, String> customClaims, TenantDTO tenant) {
    log.info("Creating user from claims");
    User user = new User();
    user.setStatus(customClaims.get("status"));
    user.setRole(customClaims.get("role"));
    user.setViewOnlyAssignedCourses(customClaims.get("viewOnlyAssignedCourses"));
    user.setLoginOption(customClaims.get("loginOption"));
    user.setLastLoginTimestamp(customClaims.get("lastLoginTimestamp"));
    user.setPk(customClaims.get("pk"));
    user.setSk(customClaims.get("sk"));
    user.setTenant(tenant);
    user.setFirstName(customClaims.get("firstName"));
    user.setLastName(customClaims.get("lastName"));

    log.info("User created from claims");
    return user;
  }

  private String sanitizeData(String unSanitizedData) {
    log.info(unSanitizedData);
    if (unSanitizedData == null || unSanitizedData.isEmpty()) {
      return null; // Return null if input is empty or null
    }

    String sanitizedData = unSanitizedData.replaceAll("\\p{M}", "");
    String lowerCased = sanitizedData.toLowerCase();
    if (lowerCased.startsWith("http:") || lowerCased.startsWith("https:") || lowerCased.startsWith("file:")) {
      throw new SecurityException("Potential SSRF detected in input: " + sanitizedData);
    }
    return sanitizedData;
  }
}

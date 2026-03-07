package com.cognizant.lms.userservice.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.filter.JWTAuthenticationFilter;
import com.cognizant.lms.userservice.service.CognitoConfigService;
import com.cognizant.lms.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Security Configuration class for the application.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true, jsr250Enabled = true, proxyTargetClass = true)
public class SecurityConfig {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RoleDao roleDao;

  @Autowired
  private UserFilterSortDao userFilterSortDao;

  @Autowired
  private UserService userService;

  @Value("${AWS_COGNITO_CLIENT_ID}")
  private String awsCognitoClientId;
  @Value("${AWS_COGNITO_ISSUER}")
  private String awsCognitoIssuer;
  @Value("${AWS_COGNITO_CERT_URL}")
  private String awsCognitoCertUrl;

  @Autowired
  private CognitoConfigService cognitoConfigService;


  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration builder)
      throws Exception {
    return builder.getAuthenticationManager();
  }

  @Bean
  protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
    AuthenticationManager manager =
        (AuthenticationManager) applicationContext.getBean("authenticationManager");
    JWTAuthenticationFilter customFilter =
        new JWTAuthenticationFilter(manager, awsCognitoCertUrl, awsCognitoClientId,
            awsCognitoIssuer, roleDao, userFilterSortDao, userService, cognitoConfigService);
    http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(
            (requests) -> requests
                .requestMatchers("/actuator/health")
                .permitAll()
              .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/email")
              .permitAll()
              .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/email/verify-otp")
              .permitAll()
              .requestMatchers(HttpMethod.POST, "/api/v1/auth/email/verify")
              .permitAll()
              .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/email/resend-otp")
              .permitAll()
              .requestMatchers(HttpMethod.POST, "/api/v1/auth/email/resend")
              .permitAll()
                .anyRequest()
                .authenticated()
            )
        .addFilter(customFilter)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
        .cors(cors -> cors.configure(http))
        .httpBasic(withDefaults())
            .headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .preload(true)
                            .maxAgeInSeconds(31536000))
                    .contentSecurityPolicy((csp -> csp
                            .policyDirectives("default-src 'self'; "
                                    + "script-src 'self' 'unsafe-inline'; "
                                    + "object-src 'none'; "
                                    + "style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self'; font-src 'self'; "
                                    + "frame-ancestors 'none'; "
                                    + "form-action 'self'; "
                                    + "base-uri 'self';")))
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                            .xssProtection(xss -> xss
                            .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                            .addHeaderWriter((request, response) -> {
                              response.setHeader("Server", "");
                              log.info("Server Header set to empty");
                            }));
    return http.build();
  }

}
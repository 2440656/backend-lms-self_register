package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.UserHomeProfileDto;
import com.cognizant.lms.userservice.dto.UserPersonalDetailsDto;
import com.cognizant.lms.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("api/v1/user-profile")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/home")
	@PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> getHomeProfileDetails() {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }
            UserHomeProfileDto homeProfile = buildHomeProfile(user);

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(homeProfile);
            response.setError(null);

            log.info("Fetched home profile details for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            log.error("Authentication principal is not of type AuthUser: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        } catch (NullPointerException e) {
            log.error("Authentication or user data is null: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required");
        } catch (Exception e) {
            log.error("Error fetching home profile: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch home profile details");
        }
    }

    @GetMapping("/personal-details")
	@PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> getPersonalDetails() {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }
            UserPersonalDetailsDto personalDetails = buildPersonalDetails(user);

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(personalDetails);
            response.setError(null);

            log.info("Fetched personal details for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            log.error("Authentication principal is not of type AuthUser: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        } catch (NullPointerException e) {
            log.error("Authentication or user data is null: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required");
        } catch (Exception e) {
            log.error("Error fetching personal details: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch personal details");
        }
    }

    private User fetchCurrentActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            log.error("No authentication found in security context");
            throw new NullPointerException("No authentication found");
        }
        
        if (!(authentication.getPrincipal() instanceof AuthUser)) {
            log.error("Principal is not an instance of AuthUser: {}", authentication.getPrincipal().getClass().getName());
            throw new ClassCastException("Invalid authentication principal type");
        }
        
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        String userEmail = authUser.getUserEmail();
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("User email is null or empty in authentication");
            throw new NullPointerException("User email not found in authentication");
        }
        
        return userService.getUserByEmailId(userEmail.toLowerCase(), Constants.ACTIVE_STATUS);
    }

    private UserHomeProfileDto buildHomeProfile(User user) {
        String displayName = String.format("%s %s", user.getFirstName(), user.getLastName()).trim();
        return UserHomeProfileDto.builder()
                .userId(user.getPk())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(displayName)
                .build();
    }

    private UserPersonalDetailsDto buildPersonalDetails(User user) {
        return UserPersonalDetailsDto.builder()
                .userId(user.getPk())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailAddress(user.getEmailId())
                .country(user.getCountry())
                .institutionName(user.getInstitutionName())
                .currentRole(user.getRole())
                .build();
    }

    private ResponseEntity<HttpResponse> buildErrorResponse(HttpStatus status, String errorMessage) {
        HttpResponse response = new HttpResponse();
        response.setStatus(status.value());
        response.setData(null);
        response.setError(errorMessage);
        return ResponseEntity.status(status).body(response);
    }
}

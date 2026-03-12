package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.AspirationalDataResponseDto;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.ProfilePhotoUploadResponse;
import com.cognizant.lms.userservice.dto.UpdatePersonalDetailsDto;
import com.cognizant.lms.userservice.dto.UpdateProfileAspirationsDto;
import com.cognizant.lms.userservice.dto.UserHomeProfileDto;
import com.cognizant.lms.userservice.dto.UserPersonalDetailsDto;
import com.cognizant.lms.userservice.service.AspirationalRoleService;
import com.cognizant.lms.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("api/v1/user-profile")
public class UserProfileController {

    private final UserService userService;
    private final AspirationalRoleService aspirationalRoleService;

    public UserProfileController(UserService userService, AspirationalRoleService aspirationalRoleService) {
        this.userService = userService;
        this.aspirationalRoleService = aspirationalRoleService;
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
        
        // Get presigned URL for profile photo if it exists
        String profilePhotoUrl = null;
        try {
            if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().trim().isEmpty()) {
                profilePhotoUrl = userService.getPresignedProfilePhotoUrl(user.getProfilePhotoUrl());
            }
        } catch (Exception e) {
            log.error("Error generating presigned URL for profile photo: {}", e.getMessage());
            profilePhotoUrl = null; // Return null if presigning fails
        }
        
        return UserHomeProfileDto.builder()
                .userId(user.getPk())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(displayName)
                .profilePhotoUrl(profilePhotoUrl)
                .build();
    }

    private UserPersonalDetailsDto buildPersonalDetails(User user) {
        // Parse comma-separated strings into lists
        List<String> selectedInterests = null;
        if (user.getSelectedInterests() != null && !user.getSelectedInterests().trim().isEmpty()) {
            selectedInterests = java.util.Arrays.asList(user.getSelectedInterests().split(","));
        }

        List<String> selectedRoles = null;
        if (user.getSelectedRoles() != null && !user.getSelectedRoles().trim().isEmpty()) {
            selectedRoles = java.util.Arrays.asList(user.getSelectedRoles().split(","));
        }

        return UserPersonalDetailsDto.builder()
                .userId(user.getPk())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailAddress(user.getEmailId())
                .country(user.getCountry())
                .institutionName(user.getInstitutionName())
                .currentRole(user.getCurrentRole() != null ? user.getCurrentRole() : user.getRole()) // Use currentRole if available, fallback to role
                .selectedUserRole(user.getSelectedUserRole())
                .selectedInterests(selectedInterests)
                .selectedRoles(selectedRoles)
                .build();
    }

    private ResponseEntity<HttpResponse> buildErrorResponse(HttpStatus status, String errorMessage) {
        HttpResponse response = new HttpResponse();
        response.setStatus(status.value());
        response.setData(null);
        response.setError(errorMessage);
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/aspirational-data")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> getAspirationalData() {
        try {
            log.info("Fetching aspirational data for dropdowns");
            AspirationalDataResponseDto aspirationalData = aspirationalRoleService.getAllAspirationalData();

            log.info("Aspirational data fetched - UserRoles: {}, Interests: {}, Roles: {}", 
                    aspirationalData.getUserRoles().size(), 
                    aspirationalData.getInterests().size(), 
                    aspirationalData.getRoles().size());

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(aspirationalData);
            response.setError(null);

            log.info("Successfully fetched aspirational data");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching aspirational data: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch aspirational data");
        }
    }

    @GetMapping("/aspirational-data/search")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> searchAspirationalData(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String query,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String type) {
        try {
            log.info("Searching aspirational data with query: '{}', type: '{}'", query, type);
            AspirationalDataResponseDto allData = aspirationalRoleService.getAllAspirationalData();
            AspirationalDataResponseDto filteredData = aspirationalRoleService.searchAspirationalData(allData, query, type);

            log.info("Search results - UserRoles: {}, Interests: {}, Roles: {}", 
                    filteredData.getUserRoles().size(), 
                    filteredData.getInterests().size(), 
                    filteredData.getRoles().size());

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(filteredData);
            response.setError(null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching aspirational data: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search aspirational data");
        }
    }

    @PutMapping("/personal-details")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> updatePersonalDetails(@RequestBody UpdatePersonalDetailsDto personalDetailsDto) {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }

            log.info("Updating personal details for user: {}", user.getEmailId());
            
            // Update user personal details in database
            userService.updateUserPersonalDetails(
                user.getPk(), 
                user.getSk(), 
                personalDetailsDto.getFirstName(),
                personalDetailsDto.getLastName(),
                personalDetailsDto.getCountry(),
                personalDetailsDto.getInstitutionName(),
                personalDetailsDto.getCurrentRole()
            );

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData("Personal details updated successfully");
            response.setError(null);

            log.info("Successfully updated personal details for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            log.error("Authentication principal is not of type AuthUser: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        } catch (NullPointerException e) {
            log.error("Authentication or user data is null: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required");
        } catch (Exception e) {
            log.error("Error updating personal details: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update personal details");
        }
    }

    @PutMapping("/aspirations")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> updateUserAspirations(@RequestBody UpdateProfileAspirationsDto aspirationsDto) {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }

            log.info("Updating aspirations for user: {}", user.getEmailId());
            aspirationalRoleService.updateUserAspirations(user.getPk(), user.getSk(), aspirationsDto);

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData("Aspirations updated successfully");
            response.setError(null);

            log.info("Successfully updated aspirations for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            log.error("Authentication principal is not of type AuthUser: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        } catch (NullPointerException e) {
            log.error("Authentication or user data is null: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required");
        } catch (Exception e) {
            log.error("Error updating user aspirations: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update aspirations");
        }
    }

    @PostMapping("/profile-photo")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }

            // Validate file
            if (file.isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "File is empty");
            }

            // Validate file type (only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Only image files are allowed");
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "File size must be less than 5MB");
            }

            log.info("Uploading profile photo for user: {}", user.getEmailId());
            
            // Upload to S3 and get URL
            String photoUrl = userService.uploadProfilePhoto(user.getPk(), user.getSk(), file);

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(ProfilePhotoUploadResponse.builder()
                    .photoUrl(photoUrl)
                    .message("Profile photo uploaded successfully")
                    .success(true)
                    .build());
            response.setError(null);

            log.info("Successfully uploaded profile photo for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading profile photo: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload profile photo");
        }
    }

    @GetMapping("/profile-photo")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> getProfilePhoto() {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }

            String storedPhotoUrl = user.getProfilePhotoUrl();
            String presignedUrl = null;
            
            if (storedPhotoUrl != null && !storedPhotoUrl.trim().isEmpty()) {
                // Generate presigned URL for access
                presignedUrl = userService.getPresignedProfilePhotoUrl(storedPhotoUrl);
            }

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(ProfilePhotoUploadResponse.builder()
                    .photoUrl(presignedUrl)
                    .message(presignedUrl != null ? "Profile photo found" : "No profile photo set")
                    .success(presignedUrl != null)
                    .build());
            response.setError(null);

            log.info("Fetched profile photo URL for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            log.error("Authentication principal is not of type AuthUser: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        } catch (NullPointerException e) {
            log.error("Authentication or user data is null: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required");
        } catch (Exception e) {
            log.error("Error fetching profile photo: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch profile photo");
        }
    }

    @DeleteMapping("/profile-photo")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
    public ResponseEntity<HttpResponse> deleteProfilePhoto() {
        try {
            User user = fetchCurrentActiveUser();
            if (user == null) {
                log.error("User not found or inactive");
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found or inactive");
            }

            String currentPhotoUrl = user.getProfilePhotoUrl();
            if (currentPhotoUrl == null || currentPhotoUrl.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "No profile photo to delete");
            }

            log.info("Deleting profile photo for user: {}", user.getEmailId());
            
            // Delete from S3 and update DB
            userService.deleteProfilePhoto(user.getPk(), user.getSk(), currentPhotoUrl);

            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK.value());
            response.setData(ProfilePhotoUploadResponse.builder()
                    .photoUrl(null)
                    .message("Profile photo deleted successfully")
                    .success(true)
                    .build());
            response.setError(null);

            log.info("Successfully deleted profile photo for user: {}", user.getEmailId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting profile photo: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete profile photo");
        }
    }
}

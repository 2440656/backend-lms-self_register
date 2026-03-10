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
//	@PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
	public ResponseEntity<HttpResponse> getHomeProfileDetails() {
		User user = fetchCurrentActiveUser();
		UserHomeProfileDto homeProfile = buildHomeProfile(user);

		HttpResponse response = new HttpResponse();
		response.setStatus(HttpStatus.OK.value());
		response.setData(homeProfile);
		response.setError(null);

		log.info("Fetched home profile details for user: {}", user.getEmailId());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/personal-details")
//	@PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','learner','mentor')")
	public ResponseEntity<HttpResponse> getPersonalDetails() {
		User user = fetchCurrentActiveUser();
		UserPersonalDetailsDto personalDetails = buildPersonalDetails(user);

		HttpResponse response = new HttpResponse();
		response.setStatus(HttpStatus.OK.value());
		response.setData(personalDetails);
		response.setError(null);

		log.info("Fetched personal details for user: {}", user.getEmailId());
		return ResponseEntity.ok(response);
	}

	private User fetchCurrentActiveUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		AuthUser authUser = (AuthUser) authentication.getPrincipal();
		String userEmail = authUser.getUserEmail().toLowerCase();
		return userService.getUserByEmailId(userEmail, Constants.ACTIVE_STATUS);
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
}

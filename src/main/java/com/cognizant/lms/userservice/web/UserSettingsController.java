package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.UserSettingsRequestDto;
import com.cognizant.lms.userservice.service.UserSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("api/v1/user-settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping("/emailId")
    @PreAuthorize("hasAnyRole('learner','content-author','system-admin','super-admin')")
    public ResponseEntity<HttpResponse> getUserSettingsByEmailId(@RequestParam String email) {
        UserSettings userSettings = userSettingsService.getUserSettingsByEmailId(email);
        HttpResponse response = new HttpResponse();
        if (userSettings == null) {
            response.setData(null);
            response.setStatus(HttpStatus.OK.value());
            response.setError("User settings not found for emailId: " + email);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        log.info("Fetching user {} by emailId", userSettings.getEmailId());
        response.setData(userSettings);
        response.setStatus(HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/createUserSettings")
    @PreAuthorize("hasAnyRole('learner','content-author','system-admin','super-admin')")
    public ResponseEntity<String> createUserSettings(@RequestBody UserSettingsRequestDto request) {
        UserSettings userSettings = userSettingsService.createUserSettings(request);
        if (userSettings != null) {
            return ResponseEntity.ok("User settings created successfully.");
        } else {
            return ResponseEntity.badRequest().body("Failed to create user settings.");
        }
    }

    @PostMapping("/migrate-type-field")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> migrateUserTypeField(@RequestParam(value = "lastEvaluatedKey", required = false) String lastEvaluatedKeyEncoded,
                                                       @RequestParam(value = "limit", defaultValue = "500") int limit) {
        HttpResponse response = userSettingsService.migrateUserTypeField(lastEvaluatedKeyEncoded, limit);
        return ResponseEntity.ok(response);
    }
}
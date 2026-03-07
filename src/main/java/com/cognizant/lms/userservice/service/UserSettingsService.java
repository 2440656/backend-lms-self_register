package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.UserSettingsRequestDto;

public interface UserSettingsService {

    UserSettings getUserSettingsByEmailId(String emailId);

    UserSettings createUserSettings(UserSettingsRequestDto requestDto);

    HttpResponse migrateUserTypeField(String lastEvaluatedKey, int limit);
}

package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Repository
public interface UserSettingsDao {

    UserSettings getUserSettingsByEmailId(String emailId, String sk);

    void saveUserSettings(UserSettings userSettings);

    Map<String,AttributeValue> updateEmptyTypeFields(Map<String, String> lastEvaluatedKey, int limit);

}

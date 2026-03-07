package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserSettingsDao;
import com.cognizant.lms.userservice.domain.UserSettings;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UserSettingsRequestDto;
import com.cognizant.lms.userservice.exception.UserSettingAlreadyFoundException;
import com.cognizant.lms.userservice.utils.Base64Util;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.cognizant.lms.userservice.constants.Constants.TYPE_USER_SETTINGS;

@Service
@Slf4j
public class UserSettingsServiceImpl implements UserSettingsService{

    private final UserSettingsDao userSettingsDao;

    public UserSettingsServiceImpl(UserSettingsDao userSettingsDao) {
        this.userSettingsDao = userSettingsDao;
    }
    @Override
    public UserSettings getUserSettingsByEmailId(String emailId) {
        emailId = emailId.toLowerCase();
        String tenantCode = TenantUtil.getTenantCode();
        String sk = tenantCode + "#" + TYPE_USER_SETTINGS;
        return userSettingsDao.getUserSettingsByEmailId(emailId, sk);
    }

    @Override
    public UserSettings createUserSettings(UserSettingsRequestDto requestDto) {
        if (requestDto.getUserId()==null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        TenantDTO tenantDTO = TenantUtil.getTenantDetails();
        log.info("Tenant details fetched for tenantCode: {}", tenantDTO.toString());
        String emailId = UserContext.getUserEmail().toLowerCase();
        String sk = tenantDTO.getPk() + "#"+ TYPE_USER_SETTINGS;
        UserSettings existing = userSettingsDao.getUserSettingsByEmailId(emailId, sk);
        if (existing != null) {
            log.info("User settings already exist for emailId: {}", emailId);
            throw new UserSettingAlreadyFoundException("User settings already exist for emailId: " + emailId);
        }
        UserSettings userSettings = UserSettings.builder()
                .tenantCode(tenantDTO.getPk())
                .type(TYPE_USER_SETTINGS)
                .voiceId(requestDto.getVoiceId())
                .theme(requestDto.getTheme())
                .emailId(emailId)
                .createdBy(UserContext.getCreatedBy())
                .createdDate(ZonedDateTime.now()
                        .format(DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP)))
                .pk(requestDto.getUserId())
                .sk(sk)
                .build();

        userSettingsDao.saveUserSettings(userSettings);
        log.info("Created new user settings for emailId: {}", emailId);
        return userSettings;
    }

    @Override
    public HttpResponse migrateUserTypeField(String lastEvaluatedKeyEncoded, int limit) {
        HttpResponse response = new HttpResponse();
        Map<String, String> lastEvaluatedKey = null;
        if (lastEvaluatedKeyEncoded != null) {
            try {
                lastEvaluatedKey = Base64Util.decodeEvaluatedKey(lastEvaluatedKeyEncoded);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("Invalid Base64 encoded lastEvaluatedKey");
                return response;
            }
        }
        Map<String, AttributeValue> attributeValueMap= userSettingsDao.updateEmptyTypeFields(lastEvaluatedKey, limit);
        response.setStatus(HttpStatus.OK.value());
        String encodedKey = attributeValueMap != null && !attributeValueMap.isEmpty() ? Base64Util.encodeLastEvaluatedKey(attributeValueMap) : null;
        response.setData("lastEvaluatedKey: " + encodedKey);
        return response;
    }
}

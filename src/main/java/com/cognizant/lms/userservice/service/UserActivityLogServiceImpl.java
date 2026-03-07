package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserActivityLogDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.UserActivityLogDto;
import com.cognizant.lms.userservice.exception.UserNotFoundException;
import com.cognizant.lms.userservice.utils.TokenUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@AllArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService{
    private final UserFilterSortDao userFilterSortDao;
    private final UserActivityLogDao userActivityLogDao;

    @Override
    public void saveUserActivityLog(String deviceDetails, String ipAddress, String activityType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthUser authUser = (AuthUser) auth.getPrincipal();
        String token = authUser.getToken();
        String tokenIssuedAt = TokenUtil.extractIssuedAtTimeStamp(token);
        String userId = TokenUtil.extractSubFromToken(token);
        log.info("Saving {} activity for user: {}", activityType, authUser.getUserEmail());
        User user = getUserByEmail(authUser.getUserEmail());
        UserActivityLogDto userActivityLogDto = new UserActivityLogDto();
        userActivityLogDto.setPk(user.getPk());
        String currentUtcTimeStamp = Instant.now().atOffset(java.time.ZoneOffset.UTC).toString();
        userActivityLogDto.setSk("USER-ACTIVITY#" + currentUtcTimeStamp);
        userActivityLogDto.setUserId(userId);
        userActivityLogDto.setFirstName(user.getFirstName());
        userActivityLogDto.setLastName(user.getLastName());
        userActivityLogDto.setEmailId(user.getEmailId());
        userActivityLogDto.setDeviceDetails(deviceDetails);
        userActivityLogDto.setIpAddress(ipAddress);
        userActivityLogDto.setActivityType(activityType);
        userActivityLogDto.setStatus(Constants.USER_ACTIVITY_SUCCESS_STATUS);
        userActivityLogDto.setTimestamp(tokenIssuedAt);
        userActivityLogDao.saveUserActivityLog(userActivityLogDto);
        log.info("{} activity saved successfully for user: {}", activityType, authUser.getUserEmail());
    }

    private User getUserByEmail(String email) {
        User existingUser = userFilterSortDao.getUserByEmailId(email, Constants.ACTIVE_STATUS);
        if (existingUser == null) {
            throw new UserNotFoundException("User not found in database with email: " + email);
        }
        return existingUser;
    }
}
package com.cognizant.lms.userservice.service;

public interface UserActivityLogService {
    void saveUserActivityLog(String deviceDetails, String ipAddress, String activityType);
}

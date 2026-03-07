package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.UserActivityLogDto;

import java.util.List;

public interface UserActivityLogDao {
    void saveUserActivityLog(UserActivityLogDto userActivityLogDto);

    List<UserActivityLogDto> findByTimestamp(String pk, String timestamp);
}

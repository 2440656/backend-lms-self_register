package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.UserAuditLogDto;

public interface UserAuditLogDao {

    UserAuditLogDto addUserAuditLog(UserAuditLogDto auditLogDto);

}

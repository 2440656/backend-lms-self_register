package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.AddUserEventDetailDto;
import com.cognizant.lms.userservice.dto.SkillMigrationEventDetailDto;

public interface UserManagementEventPublisherService {

  void triggerAddUserPublishEvent(AddUserEventDetailDto addUserEventDetailDto);

  void triggerSkillMigrationEvent(SkillMigrationEventDetailDto skillMigrationEventDetailDto);

}

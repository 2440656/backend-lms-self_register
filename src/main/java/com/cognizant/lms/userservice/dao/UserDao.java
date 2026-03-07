package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UpdateDateDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDao {

  void createUser(User user);

  boolean userExists(String emailId);

  TenantDTO getTenantDetails(String tenantIdentifier);

  List<User> getAllUsers();

  void updateLastLoginTimeStampAndPasswordChangedDate(String pk, String sk,UpdateDateDTO updateDateDTO);



  void addPasswordChangedDate(String pk, String sk,String emailId, String passwordChangedDate);
}

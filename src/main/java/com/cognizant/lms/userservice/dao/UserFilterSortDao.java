package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.UserEmailDto;
import com.cognizant.lms.userservice.dto.UserListResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFilterSortDao {

  UserListResponse getUsers(String sortKey, String order,
                            Map<String, String> lastEvaluatedKey,
                            int perPage, String userRole, String institutionName,
                            String searchValue, String status);

  List<String> getInstitutions(String sortKey);

  User getUserByEmailId(String emailId, String status);

  boolean deactivateUser(User user, String modifiedBy);

  boolean reActivateUser(User user, String newExpiryDate );

  boolean updateUser(User user, String modifiedBy);

  User getUserByPk(String partitionKeyValue);

  List<User> getUserByTenantCode(String partitionKeyValue);

  boolean updateUserByPk(User existingUser, String modifiedBy);

  Set<String> getExpiredUsers();

  boolean updateFirstLoggedInUser(User user, String modifiedBy);

  boolean updateTermsAccepted();

  boolean updateUserTermsAccepted(String pk,String sk,String termsAccepted, String termsAcceptedDate) ;

  Map<String, String> getUserEmailsByUsernames(List<String> usernames);

  void deleteBatchUsers(List<User> users);

  void updateUserSettings(String userId, String type, String option);

  boolean updateLearnerPreferredView(User existingUser, String preferredUI);

  boolean updateIsWatchedTutorial(String pk,String sk);

  boolean updateVideoLaunchCount(String pk, String sk);

  List<UserEmailDto> listUserEmailIdAndUserId();

  User getUserByEmailIdAndTenant(String emailId, String status, String tenantCode);

}
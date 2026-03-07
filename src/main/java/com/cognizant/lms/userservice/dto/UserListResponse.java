package com.cognizant.lms.userservice.dto;

import com.cognizant.lms.userservice.domain.User;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserListResponse {
  private List<User> userList;
  private Map<String, AttributeValue> lastEvaluatedKey;
  private int count;
}

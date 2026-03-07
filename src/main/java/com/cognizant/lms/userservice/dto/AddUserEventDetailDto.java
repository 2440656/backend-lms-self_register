package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddUserEventDetailDto {
  private String eventType;
  private String fileName;
  private String createdBy;
  private String tenantCode;
  private String idpPreferences;
  private List<String> userRoles;
  private String action;
  private String sk;
  private String pk;
  private int rowNum;
  private int batchStartRow;
  private int batchEndRow;

}
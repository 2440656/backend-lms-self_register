package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.OperationsHistoryFilterSortDao;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.dto.LogFileResponse;
import com.cognizant.lms.userservice.dto.OperationHistoryResponse;
import com.cognizant.lms.userservice.utils.Base64Util;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Slf4j
@Service
public class OperationsHistoryServiceImpl implements OperationsHistoryService {

  private OperationsHistoryFilterSortDao operationsHistoryFilterSortDao;

  @Autowired
  private UserService userService;

  public OperationsHistoryServiceImpl(
      OperationsHistoryFilterSortDao operationsHistoryFilterSortDao) {
    this.operationsHistoryFilterSortDao = operationsHistoryFilterSortDao;
  }

  public LogFileResponse getLogFileDetailsFilterByProcess(String partitionKeyValue, String sortKey,
                                                          String order, String process,
                                                          Map<String, String> lastEvaluatedKey,
                                                          int perPage) {
    LogFileResponse response;
    try {
      String email = UserContext.getUserEmail();
      response =
          operationsHistoryFilterSortDao.getLogFileLists(partitionKeyValue, sortKey, order, process, email,
              lastEvaluatedKey, perPage);
      log.info("LogFiles from table: {}", response.getLogFiles().size());
    } catch (Exception e) {
        log.error("Failed to fetch all the logFiles {} ", e.getMessage());
        throw new RuntimeException(e.getMessage());
    }
    return response;
  }

  @Override
  public OperationHistoryResponse getLogFiles(String sortKey, String order, String process, String area, String lastEvaluatedKeyEncoded, int perPage) {
      OperationHistoryResponse response = new OperationHistoryResponse();
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

      String partitionKeyValue;
      List<String> validProcesses;
      if (Constants.AREA_USER_MANAGEMENT.equals(area)) {
          partitionKeyValue = TenantUtil.getTenantCode() + Constants.USER_MANAGEMENT;
          validProcesses = List.of(Constants.ACTION_ADD, Constants.ACTION_UPDATE, Constants.ACTION_DEACTIVATE, Constants.ACTION_REACTIVATE);
      } else if (Constants.AREA_SKILL_MANAGEMENT.equals(area)) {
          partitionKeyValue = TenantUtil.getTenantCode() + Constants.SKILL_MANAGEMENT;
          validProcesses = List.of(Constants.ACTION_UPLOAD_SKILLS);
      } else {
          response.setData(null);
          response.setStatus(HttpStatus.BAD_REQUEST.value());
          response.setError("Invalid area value: " + area);
          return response;
      }

      LogFileResponse logFileResponse;
      if (process == null || process.isEmpty()) {
          logFileResponse = getLogFileDetailsFilterByProcess(partitionKeyValue, sortKey, order, null, lastEvaluatedKey, perPage);
      } else if (validProcesses.contains(process)) {
          logFileResponse = getLogFileDetailsFilterByProcess(partitionKeyValue, sortKey, order, process, lastEvaluatedKey, perPage);
      } else {
          response.setData(null);
          response.setStatus(HttpStatus.BAD_REQUEST.value());
          response.setError("Invalid process value: " + process);
          return response;
      }

      if (logFileResponse.getLogFiles() == null || logFileResponse.getLogFiles().isEmpty()) {
          log.error("No log files found for process: {}", process);
          response.setData(null);
          response.setStatus(HttpStatus.NO_CONTENT.value());
          response.setError("No log found");
          return response;
      }
      List<OperationsHistory> logFiles = logFileResponse.getLogFiles();
      Map<String, AttributeValue> lastKey = logFileResponse.getLastEvaluatedKey();
      response.setData(logFiles);
      if (lastKey != null && !lastKey.isEmpty()) {
          response.setLastEvaluatedKey(Base64Util.encodeLastEvaluatedKey(lastKey));
      }
      response.setStatus(HttpStatus.OK.value());
      response.setError(null);
      return response;
    }
}
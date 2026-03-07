package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.LogFileResponse;
import java.util.Map;

public interface OperationsHistoryFilterSortDao {

  LogFileResponse getLogFileLists(String partitionKeyValue, String sortKey, String order,
                                  String process, String userEmail,
                                  Map<String, String> lastEvaluatedKey,
                                  int perPage);

}

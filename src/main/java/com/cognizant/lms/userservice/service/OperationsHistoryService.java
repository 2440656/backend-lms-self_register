package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.OperationHistoryResponse;

public interface OperationsHistoryService {

  /**
   * Method to get log files with filtering and pagination
   *
   * @param sortKey               The key to sort the log files
   * @param order                 The order of sorting (asc/desc)
   * @param process               The process to filter log files
   * @param area                  The area to filter log files
   * @param lastEvaluatedKeyEncoded The encoded last evaluated key for pagination
   * @param perPage               Number of log files per page
   * @return OperationHistoryResponse containing log files and pagination info
   */
  OperationHistoryResponse getLogFiles(String sortKey, String order, String process, String area,
                                               String lastEvaluatedKeyEncoded, int perPage);
}

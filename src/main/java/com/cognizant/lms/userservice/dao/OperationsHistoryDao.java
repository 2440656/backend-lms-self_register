package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.OperationsHistory;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationsHistoryDao {
  void saveLogFileData(OperationsHistory logFile);

//  boolean updateLogFileDataStatus(String pk, String sk, String errorLogfileName, String status, String updatedBy, String updatedOn);
}

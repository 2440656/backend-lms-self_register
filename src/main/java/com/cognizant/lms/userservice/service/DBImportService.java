package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.TableDisplayNameDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DBImportService {

  int dbImportdata(MultipartFile file, String tableName);

  String getTableDataAsCSV(String tableName) throws IOException;

  boolean clearTableData(String tableName) throws Exception;

  int updateUserCountry(String tableName);

  List<TableDisplayNameDto> getTableDisplayNames();

  String deleteTableDataByPkSkAndTableName(String tableName, MultipartFile file);
}

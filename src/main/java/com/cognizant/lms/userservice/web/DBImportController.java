package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.TableDisplayNameDto;
import com.cognizant.lms.userservice.service.DBImportService;
import com.cognizant.lms.userservice.service.DBImportServiceImpl;
import com.cognizant.lms.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("api/v1/dbimport")
@Slf4j
public class DBImportController {

  @Autowired
  private DBImportService dbImportService;

  @Autowired
  private UserService userService;

  @PostMapping
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> dbImport(
      @RequestPart(value = "file") MultipartFile file,
      @RequestPart(value = "tableName") String tableName) throws Exception {
    if (tableName == null || tableName.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new HttpResponse("Table name is required", HttpStatus.BAD_REQUEST.value(), null));
    }
    HttpResponse response = new HttpResponse();
    log.info("Importing data to DynamoDB table {}", tableName);
    log.info("Importing data to file name {}", file.getName());
    int coutOfRecords = dbImportService.dbImportdata(file, tableName);
    log.info("Import is done {}", coutOfRecords);
    response.setStatus(HttpStatus.OK.value());
    response.setData(coutOfRecords);
    response.setError(null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/download")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<String> downloadTableAsCSV(@RequestParam String tableName) {
    try {
      String csvData = dbImportService.getTableDataAsCSV(tableName);
      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Disposition", "attachment; filename=" + tableName + ".csv");
      return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @DeleteMapping("/clearTableData")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> clearTableData(@RequestParam String tableName) {
    HttpResponse response = new HttpResponse();
    try {
      String env = System.getenv("APP_ENV");
      if (!"dev".equalsIgnoreCase(env) && !"qa".equalsIgnoreCase(env)) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setError("Operation not allowed in this environment.");
        response.setData(null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
      }
      boolean isEmptied = dbImportService.clearTableData(tableName);
      if (isEmptied) {
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        response.setData("Table Cleared");
        return ResponseEntity.ok(response);
      } else {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setError("Failed to clear table data.");
        response.setData(null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
    } catch (Exception e) {
      log.error("Error emptying table data: {}", e.getMessage());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError("An error occurred: " + e.getMessage());
      response.setData(null);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

    }
  }

  @PostMapping("/users/country/update")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> updateUserCountry(@RequestParam(value = "tableName",
          defaultValue = "${AWS_DYNAMODB_USER_TABLE_NAME}") String tableName) throws Exception {
    if (tableName == null || tableName.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(new HttpResponse("Table name is required", HttpStatus.BAD_REQUEST.value(), null));
    }
    HttpResponse response = new HttpResponse();
    log.info("Updating user country field to DynamoDB table {}", tableName);
    int totalUpdatedRecords = dbImportService.updateUserCountry(tableName);
    log.info("User country field updated successfully");
    response.setStatus(HttpStatus.OK.value());
    response.setData("Total records updated: " + totalUpdatedRecords);
    response.setError(null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/table-display-names")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> getTableDisplayNames() {
    try {
      HttpResponse response = new HttpResponse();
      List<TableDisplayNameDto> displayNames  = dbImportService.getTableDisplayNames();
      response.setStatus(HttpStatus.OK.value());
      response.setData(displayNames);
      response.setError(null);
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } catch (Exception e) {
      log.error("Error fetching table display names: {}", e.getMessage());
      HttpResponse response = new HttpResponse();
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setError(e.getMessage());
      response.setData(null);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/delete-data-by-pksk")
  @PreAuthorize("hasAnyRole('super-admin')")
  public ResponseEntity<HttpResponse> deleteTableDataByPkSkAndTableName(
      @RequestPart("file") MultipartFile file,
      @RequestPart(value = "tableName") String tableName) {
    String responseString = dbImportService.deleteTableDataByPkSkAndTableName(tableName, file);
    HttpResponse response = new HttpResponse();
    response.setStatus(HttpStatus.OK.value());
    response.setData(responseString);
    response.setError(null);
    return ResponseEntity.ok(response);
  }

}

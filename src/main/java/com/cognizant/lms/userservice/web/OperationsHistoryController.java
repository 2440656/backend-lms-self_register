package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.OperationHistoryResponse;
import com.cognizant.lms.userservice.service.OperationsHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/errorLogs")
@Slf4j
public class OperationsHistoryController {

  private final OperationsHistoryService operationsHistoryService;

  @GetMapping
  @PreAuthorize("hasAnyRole('system-admin','super-admin')")
  public ResponseEntity<OperationHistoryResponse> getAllLogFiles(
      @RequestParam(value = "sortKey",
          defaultValue = "${AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_KEY}") String sortKey,
      @RequestParam(value = "order",
          defaultValue = "${AWS_DYNAMODB_LOGFILE_TABLE_DEFAULT_SORT_ORDER}") String order,
      @RequestParam(value = "perPage", defaultValue = "${DEFAULT_ROWS_PER_PAGE}") int perPage,
      @RequestParam(value = "process", required = false) String process,
      @RequestParam(value = "area", defaultValue = Constants.AREA_USER_MANAGEMENT) String area,
      @RequestParam(value = "lastEvaluatedKey", required = false) String lastEvaluatedKeyEncoded) {
    try {
      perPage = perPage < 0 || perPage > 100 ? 5 : perPage;
      OperationHistoryResponse response =
          operationsHistoryService.getLogFiles(sortKey, order, process, area, lastEvaluatedKeyEncoded, perPage);
      if (response.getStatus() == HttpStatus.BAD_REQUEST.value()) {
        return ResponseEntity.badRequest().body(response);
      }
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to fetch all the logFiles {} ", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}

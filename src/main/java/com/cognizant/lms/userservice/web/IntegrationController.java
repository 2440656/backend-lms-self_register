package com.cognizant.lms.userservice.web;


import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationSummaryResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.dto.TestConnectionRequestDto;
import com.cognizant.lms.userservice.exception.SFTPConnectionException;
import com.cognizant.lms.userservice.exception.SFTPIntegrationAlreadyExistException;
import com.cognizant.lms.userservice.service.IntegrationService;
import com.cognizant.lms.userservice.utils.SanitizeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("api/v1/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> saveIntegration(@RequestBody IntegrationDraftRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            IntegrationDto integration = integrationService.saveIntegration(request);
            response.setData(integration);
            response.setStatus(HttpStatus.CREATED.value());
            response.setError(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error saving draft: {}", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/{lmsIntegrationId}")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> getIntegrationByLmsIntegrationId(@PathVariable String lmsIntegrationId,@RequestParam (required = false) String pageName) {
        HttpResponse response = new HttpResponse();
        try {
            IntegrationDto integration = integrationService.getIntegrationByLmsIntegrationId(lmsIntegrationId,pageName);
            if (integration == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setError("Integration not found with lmsIntegrationId: " + lmsIntegrationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                response.setData(integration);
                response.setStatus(HttpStatus.OK.value());
                response.setError(null);
                log.info("Fetched integration by lmsIntegrationId {}", lmsIntegrationId);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error fetching integration by lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("An unexpected error occurred : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<IntegrationSummaryResponse> getIntegrationSummary(
            @RequestParam(value = "sortKey", defaultValue = "updatedDate") String sortKey,
            @RequestParam(value = "order", defaultValue = "desc") String order,
            @RequestParam(value = "lastEvaluatedKey", required = false) String lastEvaluatedKeyEncoded,
            @RequestParam(value = "perPage", defaultValue = "${DEFAULT_ROWS_PER_PAGE}") int perPage,
            @RequestParam(value = "integrationType", required = false) String integrationType,
            @RequestParam(value = "searchValue", required = false) String searchValue,
            @RequestParam(value = "status", required = false) String status) {
        try {
            IntegrationSummaryResponse response = integrationService.getIntegrationSummary(
                    sortKey, order, lastEvaluatedKeyEncoded, perPage, integrationType, searchValue, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            IntegrationSummaryResponse errorResponse = new IntegrationSummaryResponse();
            errorResponse.setStatus(500);
            errorResponse.setError("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> testConnection(@RequestBody TestConnectionRequestDto request) {
        HttpResponse response = new HttpResponse();
        try {
            String rawId = request.getId();
            if (rawId == null || !rawId.matches("^[A-Za-z0-9_-]{1,64}$")) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("Invalid id");
                return ResponseEntity.badRequest().body(response);
            }
            String safeId = SanitizeUtil.sanitizeOriginalData(rawId);

            HttpStatusCode integration = integrationService.testConnection(safeId, SanitizeUtil.sanitizeOriginalData(request.getClientId()), SanitizeUtil.sanitizeOriginalData(request.getClientCode()));
            response.setData(integration);
            response.setStatus(integration.value());
            response.setError(null);
            return ResponseEntity.status(integration.value()).body(response);

        } catch (Exception e) {
            log.error("Error in test connection: ");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("An unexpected error occurred while testing the connection.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/sftp")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> saveSFTPIntegration(@RequestBody SFTPIntegrationReqDto request) {
        HttpResponse response = new HttpResponse();
        try {
            SFTPIntegrationResponseDto integration = integrationService.saveSFTPIntegration(request);
            response.setData(integration);
            response.setStatus(HttpStatus.CREATED.value());
            response.setError(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SFTPIntegrationAlreadyExistException e) {
            log.info("SFTP already exist: {}", e.getMessage());
            response.setStatus(HttpStatus.OK.value());
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error in test connection: {}", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/sftp/{lmsIntegrationId}")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> getSftpIntegrationByLmsIntegrationId(@PathVariable String lmsIntegrationId,
                                                                             @RequestParam (required = false) String pageName,
                                                                             @RequestParam (required = false, defaultValue = "false") boolean edit) {
        HttpResponse response = new HttpResponse();
        try {
            SFTPIntegrationResponseDto sftpIntegrationResponseDto = integrationService.getSftpIntegrationByLmsIntegrationId(lmsIntegrationId,pageName,edit);
            if (sftpIntegrationResponseDto == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setError("sftp Integration not found with lmsIntegrationId: " + lmsIntegrationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                response.setData(sftpIntegrationResponseDto);
                response.setStatus(HttpStatus.OK.value());
                response.setError(null);
                log.info("Fetched sftp integration by lmsIntegrationId {}", lmsIntegrationId);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error while sftp fetching integration by lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("An unexpected error occurred : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/sftpTestConnection")
    @PreAuthorize("hasAnyRole('super-admin','system-admin')")
    public ResponseEntity<HttpResponse> sftpTestConnection(@RequestParam(value = "sftpUserName") String sftpUserName,
                                                           @RequestParam(value = "sftpPassword") String sftpPassword,
                                                           @RequestParam(value = "sftpLocation") String sftpLocation,
                                                           @RequestParam(value = "sftpPort") String sftpPort,
                                                           @RequestParam(value = "sftpHost") String sftpHost
    ) {
        HttpResponse response = new HttpResponse();
        try {
            String integration = integrationService.sftpTestConnection(sftpUserName, sftpPassword,
                sftpLocation, sftpPort, sftpHost);
            response.setData(integration);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.status(HttpStatus.OK.value()).body(response);

        } catch (SFTPConnectionException e) {
            log.error("SFTP connection error: {}", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error in test connection: {}", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

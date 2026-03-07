package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationSummaryResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import org.springframework.http.HttpStatusCode;

import java.io.IOException;


public interface IntegrationService {


    /**
     * Get integration by partition key value.
     * @param lmsIntegrationId
     * @param pageName
     * @return IntegrationDto
     */
    IntegrationDto getIntegrationByLmsIntegrationId(String lmsIntegrationId,String pageName);

    /**
     * Get integration summary with pagination and filtering options.
     * @param sortKey The key to sort the results by.
     * @param order The order of sorting (e.g., ascending or descending).
     * @param lastEvaluatedKeyEncoded The encoded last evaluated key for pagination.
     * @param perPage The number of items per page.
     * @param status The status filter for the integrations.
     * @param integrationType The type of integration to filter by.
     * @param searchValue The search value to filter the results.
     * @return IntegrationSummaryResponse containing the list of integrations and pagination info.
     */
    IntegrationSummaryResponse getIntegrationSummary(String sortKey, String order,
                                                     String lastEvaluatedKeyEncoded,
                                                     int perPage, String status,
                                                     String integrationType, String searchValue);

    /**
     * Save integration details.
     * @param request
     * @return
     */
    IntegrationDto saveIntegration(IntegrationDraftRequest request);

    /**
     * Third party integrations test connection
     * @param id
     * @param clientId
     * @param clientCode
     * @return
     */
    HttpStatusCode testConnection(String id, String clientId, String clientCode);

    SFTPIntegrationResponseDto saveSFTPIntegration(SFTPIntegrationReqDto request);

    SFTPIntegrationResponseDto getSftpIntegrationByLmsIntegrationId(String lmsIntegrationId, String pageName, boolean edit);

    String sftpTestConnection(String sftpUserName, String sftpPassword, String sftpLocation, String sftpPort, String sftpHost) throws IOException;
}

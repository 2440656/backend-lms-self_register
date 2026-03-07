package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationListResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;

import java.util.List;
import java.util.Map;

public interface IntegrationDao {

    IntegrationDto getIntegrationByPartitionKey(String partitionKeyValue);

    IntegrationListResponse getIntegrations(String sortKey, String order,
                                            Map<String, String> lastEvaluatedKey,
                                            int perPage, String status, String integrationType, String searchValue);

    void saveGeneralInformation(String pk,String sk, String type, String status, IntegrationDraftRequest request);

    void saveCoreConfigurationAdditionalFields(String pk, List<IntegrationDraftRequest.FieldEntry> fields);

    void saveCoreConfiguration(String pk,String sk, String status, IntegrationDraftRequest request);

    void saveSettings(String partitionKey, String sortKey, String status, IntegrationDraftRequest request);

    void saveContentMapping(String partitionKey, String sortKey, String status, IntegrationDraftRequest request);

    void saveMetaDataMapping(String partitionKey, String sortKey, String status, IntegrationDraftRequest request);

    void saveLessonMapping(String partitionKey, String sortKey, String status, IntegrationDraftRequest request);


    void saveSFTPConfiguration(String partitionKey, String sortKey, String type, String status, SFTPIntegrationReqDto request);

    SFTPIntegrationResponseDto getSFTPIntegrationByPartitionKey(String partitionKeyValue, boolean edit);

    void saveSftpSettings(String partitionKey, String sortKey, String status, SFTPIntegrationReqDto request);

    void saveSftpCategoryMapping(String partitionKey, String sortKey, String status, SFTPIntegrationReqDto request);

    IntegrationListResponse fetchExistingIntegration(String sortKey, String status, String integrationType);

    void updateSFTPConfiguration(String partitionKey, String sortKey, String status, SFTPIntegrationReqDto request);
}

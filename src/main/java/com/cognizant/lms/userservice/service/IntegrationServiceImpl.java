package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.client.CourseManagementServiceClient;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.IntegrationDao;

import com.cognizant.lms.userservice.dto.IntegrationDraftRequest;
import com.cognizant.lms.userservice.dto.IntegrationDto;
import com.cognizant.lms.userservice.dto.IntegrationListResponse;
import com.cognizant.lms.userservice.dto.IntegrationSummaryResponse;
import com.cognizant.lms.userservice.dto.SFTPIntegrationReqDto;
import com.cognizant.lms.userservice.dto.SFTPIntegrationResponseDto;
import com.cognizant.lms.userservice.exception.DataBaseException;

import com.cognizant.lms.userservice.exception.SFTPConnectionException;
import com.cognizant.lms.userservice.exception.SFTPIntegrationAlreadyExistException;
import com.cognizant.lms.userservice.utils.Base64Util;
import com.cognizant.lms.userservice.utils.SFTPUtil;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.CommonUtil;
import com.cognizant.lms.userservice.validations.CoreConfigurationValidator;
import com.cognizant.lms.userservice.validations.ContentMappingValidator;
import com.cognizant.lms.userservice.validations.GeneralInformationValidator;
import com.cognizant.lms.userservice.validations.MetadataMappingValidator;
import com.cognizant.lms.userservice.validations.SFTPIntegrationValidator;
import com.cognizant.lms.userservice.validations.SettingsValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.*;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IntegrationService{

    private final IntegrationDao integrationDao;

    private final GeneralInformationValidator generalInformationValidator;

    private final CoreConfigurationValidator coreConfigurationValidator;

    private final SettingsValidator settingsValidator;

    private final ContentMappingValidator contentMappingValidator;

    private final MetadataMappingValidator metadataMappingValidator;

    private final CourseManagementServiceClient courseManagementServiceClient;

  private final SFTPUtil sftpUtil;

    private final SFTPIntegrationValidator sftpIntegrationValidator;

    public IntegrationSummaryResponse getIntegrationSummary(String sortKey, String order,
                                                            String lastEvaluatedKeyEncoded,
                                                            int perPage, String status,
                                                            String integrationType, String searchValue) {
        IntegrationSummaryResponse response = new IntegrationSummaryResponse();
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
        if (searchValue != null && searchValue.length() < 3) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Value must contain at least 3 characters");
            return response;
        }

        IntegrationListResponse result = integrationDao.getIntegrations(
                sortKey, order, lastEvaluatedKey, perPage, status, integrationType, searchValue);

        List<IntegrationDto> integrationList = result.getIntegrationList();
        Map<String, AttributeValue> lastKey = result.getLastEvaluatedKey();
        int totalCount = result.getCount();

        response.setData(integrationList);
        response.setStatus(integrationList.isEmpty() ? HttpStatus.NO_CONTENT.value() : HttpStatus.OK.value());
        response.setError(integrationList.isEmpty() ? "No integrations found" : null);
        response.setCount(totalCount);
        if (lastKey != null && !lastKey.isEmpty()) {
            response.setLastEvaluatedKey(Base64Util.encodeLastEvaluatedKey(lastKey));
        }
        return response;
    }


    public IntegrationDto getIntegrationByLmsIntegrationId(String lmsIntegrationId,String pageName) {
        log.info("Fetching integration for lmsIntegrationId: {}", lmsIntegrationId);
        try {
            String partitionKeyValue = TenantUtil.getTenantCode() + Constants.HASH + lmsIntegrationId;
            IntegrationDto integration = integrationDao.getIntegrationByPartitionKey(partitionKeyValue);
            if (integration == null) {
                log.warn("No integration found for lmsIntegrationId: {}", lmsIntegrationId);
                throw new DataBaseException("Integration not found for lmsIntegrationId: " + lmsIntegrationId);
            }
            log.info("Successfully fetched integration for lmsIntegrationId: {}", lmsIntegrationId);
            if (Constants.GENERAL_INFORMATION_PAGE.equalsIgnoreCase(pageName)) {

                return IntegrationDto.builder().provider(integration.getProvider())
                        .integrationId(integration.getIntegrationId())
                        .integrationOwner(integration.getIntegrationOwner())
                        .status(integration.getStatus())
                        .reasonForChange(integration.getReasonForChange())
                        .build();

            } else if (Constants.CORE_CONFIGURATION_PAGE.equalsIgnoreCase(pageName)) {

               return IntegrationDto.builder().hostName(integration.getHostName())
                        .clientId(integration.getClientId())
                        .organizationId(integration.getOrganizationId())
                        .clientSecret(integration.getClientSecret())
                        .testConnection(integration.getTestConnection())
                        .fields(integration.getFields())
                        .build();

            } else if(Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName))
            {
                return IntegrationDto.builder()
                        .authenticationMethod(integration.getAuthenticationMethod())
                        .syncType(integration.getSyncType())
                        .syncSchedule(integration.getSyncSchedule())
                        .weekDay(integration.getWeekDay())
                        .syncTime(integration.getSyncTime())
                        .identifiersList(integration.getIdentifiersList())
                        .build();

            }
            else if(Constants.CONTENT_MAPPING_PAGE.equalsIgnoreCase(pageName)){
                return IntegrationDto.builder()
                        .contentTypeMapping(integration.getContentTypeMapping())
                        .categoryMappingType(integration.getCategoryMappingType())
                        .categoryName(integration.getCategoryName())
                        .categoryTypeMapping(integration.getCategoryTypeMapping())
                        .completionSyncMapping(integration.getCompletionSyncMapping())
                        .build();
            }
            else if(Constants.METADATA_MAPPING_PAGE.equalsIgnoreCase(pageName))
            {
                return IntegrationDto.builder()
                        .prefix(integration.getPrefix())
                        .metaDataMappings(integration.getMetaDataMappings())
                        .lessonMetaDataMappings(integration.getLessonMetaDataMappings())
                        .build();
            }
//            else if(Constants.LESSON_MAPPING_PAGE.equalsIgnoreCase(pageName))
//            {
//                return IntegrationDto.builder()
//                        .prefix(integration.getPrefix())
//                        .lessonMetadataMappings(integration.getLessonMetadataMappings())
//                        .build();
//            }
            else {
                log.info("whole object: {}", integration);
                return integration;


            }

        } catch (DataBaseException e) {
            log.error("Database error fetching integration for lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
            throw e;
        }
        catch(NullPointerException e)
        {
            log.error("Null pointer exception fetching integration for lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
            throw new DataBaseException("Integration not found for lmsIntegrationId: " + lmsIntegrationId);
        }
        catch (Exception e) {


            log.error("Error fetching integration for lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
            throw new DataBaseException("Error fetching integration for lmsIntegrationId: " + lmsIntegrationId);
        }
    }


    public IntegrationDto saveIntegration(IntegrationDraftRequest request) {
        String pageName = request.getPageName();

        if (!Constants.GENERAL_INFORMATION_PAGE.equalsIgnoreCase(pageName)
                && !Constants.CORE_CONFIGURATION_PAGE.equalsIgnoreCase(pageName)
                && !Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)
                && !Constants.CONTENT_MAPPING_PAGE.equalsIgnoreCase(pageName)
                && !Constants.METADATA_MAPPING_PAGE.equalsIgnoreCase(pageName)

        ) {
            log.error("Invalid page name: {}", pageName);
            throw new IllegalArgumentException("Invalid page name: " + pageName);
        }

        if (Constants.GENERAL_INFORMATION_PAGE.equalsIgnoreCase(pageName)) {

            if (request.getLmsIntegrationId() == null) {
                String generatedId = CommonUtil.generateUniqueId();
                request.setLmsIntegrationId(generatedId);
                log.info("Generated new lmsIntegrationId: {}", generatedId);
            }
            if(request.getAction() == null) {
                generalInformationValidator.validate(request.getGeneralInformation());
            }

        } else if (Constants.CORE_CONFIGURATION_PAGE.equalsIgnoreCase(pageName)) {
            if (request.getLmsIntegrationId() == null) {
                log.error("lmsIntegrationId must be provided for core configuration page");
                throw new IllegalArgumentException("lmsIntegrationId must be provided for core configuration page");
            }
            if(request.getAction() == null) {
                coreConfigurationValidator.validate(request.getCoreConfiguration());
            }

        }
        else if(Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)){
            if (request.getLmsIntegrationId() == null) {
                log.error("lmsIntegrationId must be provided for Setting page");
                throw new IllegalArgumentException("lmsIntegrationId must be provided for core configuration page");
            }
            if(request.getAction() == null) {
                settingsValidator.validate(request.getSettings());
            }

        }
        else if(Constants.CONTENT_MAPPING_PAGE.equalsIgnoreCase(pageName)){
            if (request.getLmsIntegrationId() == null) {
                log.error("lmsIntegrationId must be provided for Category Type Mapping page");
                throw new IllegalArgumentException("lmsIntegrationId must be provided for Category Type Mapping page");
            }
            if(request.getAction() == null) {
                contentMappingValidator.validate(request.getContentMapping());
            }

        }
        else if(Constants.METADATA_MAPPING_PAGE.equalsIgnoreCase(pageName)){
            if (request.getLmsIntegrationId() == null) {
                log.error("lmsIntegrationId must be provided for Lesson Metadata Mapping page");
                throw new IllegalArgumentException("lmsIntegrationId must be provided for Metadata Mapping page");
            }
            if(request.getAction() == null) {
                metadataMappingValidator.validate(request.getMetaData());
            }

        }
//        else if(Constants.LESSON_MAPPING_PAGE.equalsIgnoreCase(pageName)){
//            if (request.getLmsIntegrationId() == null) {
//                log.error("lmsIntegrationId must be provided for Lesson Metadata Mapping page");
//                throw new IllegalArgumentException("lmsIntegrationId must be provided for Metadata Mapping page");
//            }
//            if(request.getAction() == null) {
//                metadataMappingValidator.validate(request.getMetaData());
//            }
//
//        }

        String partitionKey = TenantUtil.getTenantCode() + Constants.HASH + request.getLmsIntegrationId();
        String sortKey = request.getIntegrationType() + Constants.HASH + Constants.INTEGRATION_TYPE;
        String type = Constants.INTEGRATION_TYPE;
        // Set status based on action
        String status;
        if ("saveAsDraft".equalsIgnoreCase(request.getAction())) {
            status = Constants.DRAFT_STATUS;
        } else if (request.getStatus() != null) {
            status = request.getStatus();
        } else {
            status = Constants.DRAFT_STATUS; // fallback default
        }

        try {
            if (Constants.GENERAL_INFORMATION_PAGE.equalsIgnoreCase(pageName)) {
                integrationDao.saveGeneralInformation(partitionKey, sortKey, type, status, request);
            } else if( Constants.CORE_CONFIGURATION_PAGE.equalsIgnoreCase(pageName)) {
                integrationDao.saveCoreConfiguration(partitionKey, sortKey, status, request);
            }
            else if(Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)){
                integrationDao.saveSettings(partitionKey, sortKey, status, request);
            }
            else if(Constants.CONTENT_MAPPING_PAGE.equalsIgnoreCase(pageName)){
                integrationDao.saveContentMapping(partitionKey, sortKey, status, request);
            }
            else if(Constants.METADATA_MAPPING_PAGE.equalsIgnoreCase(pageName)){
                integrationDao.saveMetaDataMapping(partitionKey, sortKey, status, request);
                integrationDao.saveLessonMapping(partitionKey, sortKey, status, request);
            }
//            else if(Constants.LESSON_MAPPING_PAGE.equalsIgnoreCase(pageName))
//            {
//                integrationDao.saveLessonMapping(partitionKey, sortKey, status, request);
//            }
            else {
                log.error("Invalid page name: {}", pageName);
                throw new IllegalArgumentException("Invalid page name: " + pageName);
            }

            log.info("Draft saved successfully for lmsIntegrationId: {}", request.getLmsIntegrationId());
            return IntegrationDto.builder()
                    .lmsIntegrationId(request.getLmsIntegrationId())
                    .integrationType(request.getIntegrationType())
                    .build();
        } catch (Exception e) {
            log.error("Error saving integration draft for lmsIntegrationId {}: {}", request.getLmsIntegrationId(), e.getMessage());
            throw new DataBaseException("Error saving integration draft: " + e.getMessage());
        }
    }


  public HttpStatusCode testConnection(String id, String clientId, String clientCode){
    return courseManagementServiceClient.testConnection(id, clientId, clientCode);
  }

  @Override
  public SFTPIntegrationResponseDto saveSFTPIntegration(SFTPIntegrationReqDto request) {
    String pageName = request.getPageName();
    boolean isEditMode = request.isEdit();
    SFTPIntegrationReqDto.Configuration configuration = request.getConfiguration();
    String uniqIntegration;
    if (configuration!=null){
        uniqIntegration = request.getConfiguration().getSyncType()
                + Constants.HASH + request.getConfiguration().getHost()
                + Constants.HASH + request.getConfiguration().getLocation();
        request.setUniqIntegrationKey(uniqIntegration);
    } else {
        uniqIntegration = null;
    }
    boolean providerExists = false;
    boolean uniqIntegrationKeyExists = false;
    if (request.getIntegrationType() != null
            && pageName.equalsIgnoreCase(Constants.CONFIGURATION)
            && request.getIntegrationType().equalsIgnoreCase(Constants.SFTP_INTEGRATION_TYPE)) {
        IntegrationListResponse existingIntegrations = integrationDao.fetchExistingIntegration(Constants.UPDATED_DATE, Constants.ACTIVE_INTEGRATION, Constants.SFTP_INTEGRATION_TYPE);
        if (!existingIntegrations.getIntegrationList().isEmpty() && request.getConfiguration().getProvider() !=null) {
            if(isEditMode) {
                providerExists = existingIntegrations.getIntegrationList().stream()
                        .filter(integration -> !integration.getLmsIntegrationId().equals(request.getLmsIntegrationId()))
                        .anyMatch(integration -> integration.getProvider().equalsIgnoreCase(request.getConfiguration().getProvider()));
            } else{
                providerExists = existingIntegrations.getIntegrationList().stream()
                        .anyMatch(integration -> integration.getProvider().equalsIgnoreCase(request.getConfiguration().getProvider()));
            }
        }
        if (!existingIntegrations.getIntegrationList().isEmpty() && uniqIntegration != null && !isEditMode) {
            uniqIntegrationKeyExists = existingIntegrations.getIntegrationList().stream()
                    .filter(integration -> integration.getUniqIntegrationKey() != null)
                    .anyMatch(integration -> integration.getUniqIntegrationKey().equalsIgnoreCase(uniqIntegration));
        }
    }
    if (providerExists) {
        log.error("SFTP Integration with provider {} already exists.", request.getConfiguration().getProvider());
        throw new SFTPIntegrationAlreadyExistException("SFTP Integration with provider " + request.getConfiguration().getProvider() + " already exists.");
    }

    //uniqIntegrationKeyExists is combination of SyncType#Host#SFTPLocation
    if (uniqIntegrationKeyExists) {
      log.error("SFTP Integration with the same configuration already exists.");
      throw new SFTPIntegrationAlreadyExistException("SFTP Integration with the same configuration already exists.");
    }

    if (!Constants.CONFIGURATION.equalsIgnoreCase(pageName) && !Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)
            && !Constants.CATEGORY_MAPPING.equalsIgnoreCase(pageName)) {
      log.error("Invalid page name: {}", pageName);
      throw new IllegalArgumentException("Invalid page name: " + pageName);
    }
    if (isEditMode && !Constants.CONFIGURATION.equalsIgnoreCase(pageName)) {
        return SFTPIntegrationResponseDto.builder()
                .lmsIntegrationId(request.getLmsIntegrationId())
                .integrationType(request.getIntegrationType())
                .build();
    }
    if (isEditMode && Constants.CONFIGURATION.equalsIgnoreCase(pageName)) {
        log.info("Update started in edit mode for lmsIntegrationId: {}", request.getLmsIntegrationId());
        String partitionKey = TenantUtil.getTenantCode() + Constants.HASH + request.getLmsIntegrationId();
        String sortKey = request.getIntegrationType() + Constants.HASH + Constants.INTEGRATION_TYPE;
        integrationDao.updateSFTPConfiguration(partitionKey, sortKey, request.getStatus(), request);
        log.info("Update completed in edit mode for lmsIntegrationId: {}", request.getLmsIntegrationId());
        return SFTPIntegrationResponseDto.builder()
                .lmsIntegrationId(request.getLmsIntegrationId())
                .integrationType(request.getIntegrationType())
                .build();
    }

    if (Constants.CONFIGURATION.equalsIgnoreCase(pageName)) {
      if (request.getLmsIntegrationId() == null) {
        String generatedId = CommonUtil.generateUniqueId();
        request.setLmsIntegrationId(generatedId);
        log.info("Generated new lmsIntegrationId: {}", generatedId);
      }
      if (request.getAction() == null) {
        sftpIntegrationValidator.sftpIntegrationValidator(request.getConfiguration());
      }
      request.setVersionStatus("Draft");

    }else if(Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)){
        if (request.getLmsIntegrationId() == null) {
            log.error("lmsIntegrationId must be provided for Setting page");
            throw new IllegalArgumentException("lmsIntegrationId must be provided for Setting page");
        }
        if(request.getAction() == null) {
            sftpIntegrationValidator.sftpSettingsValidator(request.getSftpSettingsDTO());
        }
    }else if(Constants.CATEGORY_MAPPING.equalsIgnoreCase(pageName)){
        if(request.getLmsIntegrationId()==null){
            log.error("lmsIntegrationId must be provided for Category Mapping page");
            throw new IllegalArgumentException("lmsIntegrationId must be provided for Category Mapping page");
        }
        if(request.getAction()==null){
            sftpIntegrationValidator.CategoryMapperValidator(request.getCategoryMapping());
        }
        request.setVersionStatus("Published");
    }

    String partitionKey = TenantUtil.getTenantCode() + Constants.HASH + request.getLmsIntegrationId();
    String sortKey = request.getIntegrationType() + Constants.HASH + Constants.INTEGRATION_TYPE;
    String type = Constants.INTEGRATION_TYPE;
    // Set status based on action
    String status;
    if ("saveAsDraft".equalsIgnoreCase(request.getAction())) {
      status = Constants.DRAFT_STATUS;
    } else if (request.getStatus() != null) {
      status = request.getStatus();
    } else {
      status = Constants.DRAFT_STATUS; // fallback default
    }

    try {
      if (Constants.CONFIGURATION.equalsIgnoreCase(pageName)) {
        integrationDao.saveSFTPConfiguration(partitionKey, sortKey, type, status, request);
      }else if(Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)){
          integrationDao.saveSftpSettings(partitionKey, sortKey, status, request);
      }else if(Constants.CATEGORY_MAPPING.equalsIgnoreCase(pageName)){
          integrationDao.saveSftpCategoryMapping(partitionKey, sortKey, status, request);
      }else {
        log.error("Invalid page name: {}", pageName);
        throw new IllegalArgumentException("Invalid page name: " + pageName);
      }

      log.info("Draft saved successfully for lmsIntegrationId: {}", request.getLmsIntegrationId());
      return SFTPIntegrationResponseDto.builder()
          .lmsIntegrationId(request.getLmsIntegrationId())
          .integrationType(request.getIntegrationType())
          .build();
    } catch (Exception e) {
      log.error("Error saving integration draft for lmsIntegrationId {}: {}", request.getLmsIntegrationId(), e.getMessage());
      throw new DataBaseException("Error saving integration draft: " + e.getMessage());
    }
  }

  @Override
  public SFTPIntegrationResponseDto getSftpIntegrationByLmsIntegrationId(String lmsIntegrationId, String pageName, boolean edit) {
    log.info("Fetching SFTP integration for lmsIntegrationId: {}", lmsIntegrationId);
    try {
      String partitionKeyValue = TenantUtil.getTenantCode() + Constants.HASH + lmsIntegrationId;
      SFTPIntegrationResponseDto integration = integrationDao.getSFTPIntegrationByPartitionKey(partitionKeyValue, edit);
      if (integration == null) {
        log.warn("No SFTP integration found for lmsIntegrationId: {}", lmsIntegrationId);
        throw new DataBaseException("SFTP Integration not found for lmsIntegrationId: " + lmsIntegrationId);
      }
      if (edit) {
          log.info("returned whole object in case of edit mode: {}", integration);
          return integration;
      }
      log.info("Successfully fetched SFTP integration for lmsIntegrationId: {}", lmsIntegrationId);
      if (Constants.CONFIGURATION.equalsIgnoreCase(pageName)) {

        return SFTPIntegrationResponseDto.builder()
            .provider(integration.getProvider())
            .syncType(integration.getSyncType())
            .location(integration.getLocation())
            .userName(integration.getUserName())
            .password(integration.getPassword())
            .host(integration.getHost())
            .port(integration.getPort())
            .testConnection(integration.getTestConnection())
            .build();

      }else if(Constants.SETTINGS_PAGE.equalsIgnoreCase(pageName)){
          return SFTPIntegrationResponseDto.builder()
                  .syncTime(integration.getSyncTime())
                  .build();
      }else if(Constants.CATEGORY_MAPPING.equalsIgnoreCase(pageName)) {
          return SFTPIntegrationResponseDto.builder()
                  .sftpCategoryTypeMapping(integration.getSftpCategoryTypeMapping())
                  .build();
      }else {
        log.info("whole object: {}", integration);
        return integration;
      }
    } catch (DataBaseException e) {
      log.error("Database error fetching SFTP integration for lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
      throw e;
    } catch (NullPointerException e) {
      log.error("Null pointer exception fetching SFTP integration for lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
      throw new DataBaseException("SFTP Integration not found for lmsIntegrationId: " + lmsIntegrationId);
    } catch (Exception e) {
      log.error("Error fetching SFTP integration for lmsIntegrationId {}: {}", lmsIntegrationId, e.getMessage());
      throw new DataBaseException("Error fetching SFTP integration for lmsIntegrationId: " + lmsIntegrationId);
    }
  }

  @Override
  public String sftpTestConnection(String sftpUserName, String sftpPassword,
                                   String sftpLocation, String sftpPort, String sftpHost) throws SFTPConnectionException {
    try {
      return sftpUtil.checkUsernamePasswordAuthConnection(sftpUserName, sftpPassword, sftpLocation, sftpPort, sftpHost);
    } catch (SFTPConnectionException e) {
      log.error("SFTP connection error: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error during SFTP connection test: {}", e.getMessage());
      throw new SFTPConnectionException("Unexpected error during SFTP connection test: " + e.getMessage(), e);
    }
  }

}


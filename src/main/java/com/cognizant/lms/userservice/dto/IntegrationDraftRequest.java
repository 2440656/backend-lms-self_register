package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationDraftRequest {

    private String pageName;
    private String lmsIntegrationId;
    private String integrationType;
    private String action;
    private String status;
    private GeneralInformation generalInformation;
    private CoreConfiguration coreConfiguration;
    private Settings settings;
    private ContentMapping contentMapping;
    private MetaData metaData;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralInformation {
        private String provider;
        private String integrationId;
        private String integrationOwner;
        private String reasonForChange; // Optional for edit
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldEntry {
        private String fieldName;
        private String fieldValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoreConfiguration {
        private String hostName;
        private String clientId;
        private String organizationId;
        private String clientSecret;
        private String testConnection;
        private List<FieldEntry> fields;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueIdentifiers {
        private String thirdPartyIdentifier;
        private String skillSpringIdentifier;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Settings{
        private String authenticationMethod;
        private List<UniqueIdentifiers>identifiersList;
        private String syncType;
        private String syncSchedule;
        private String weekDay;
        private String syncTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentMapping {
        private List<ContentTypeMapping> contentTypeMapping;
        private String categoryMappingType;
        private String categoryName;
        private List<CategoryTypeMapping> categoryTypeMapping;
        private List<CompletionSyncMapping> completionSyncMapping;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTypeMapping {
        private String thirdPartyCategoryType;
        private String skillSpringCategoryType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentTypeMapping {
        private String thirdPartyContentType;
        private String skillSpringContentType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletionSyncMapping {
        private String thirdPartyCompletionStatus;
        private String skillSpringCompletionStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaData {
        private String prefix;
        private List<MetaDataMappings> metaDataMappings;
        private List<LessonMetaDataMappings> lessonMetaDataMappings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaDataMappings {
        private String thirdPartyMetadataField;
        private String skillSpringMetadataField;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonMetaDataMappings {
        private String thirdPartyLessonMetadataField;
        private String skillSpringLessonMetadataField;
    }



}

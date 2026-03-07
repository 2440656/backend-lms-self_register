package com.cognizant.lms.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationDto {

    private String pk;
    private String sk;
    private String pageName;
    private String lmsIntegrationId;
    // general information
    private String provider;
    private String integrationType;
    private String integrationId;
    private String status;
    private String integrationOwner;
    private String type;
    private String reasonForChange;
    private String uniqIntegrationKey;
    private String versionStatus;
    // core configuration
    private String hostName;
    private String clientId;
    private String clientSecret;
    private String organizationId;
    private String testConnection;
    private String fieldName;
    private String fieldValue;
    private String createdOn;
    private String updatedDate;
    private String createdBy;
    private String updatedBy;
    List<FieldEntry> fields;
    //    settings
    private String authenticationMethod;
    private String thirdPartyIdentifier;
    private String skillSpringIdentifier;
    private List<UniqueIdentifiers> identifiersList;
    private String syncType;
    private String syncSchedule;
    private String weekDay;
    private String syncTime;
    // content mapping
    private String thirdPartyContentType;
    private String skillSpringContentType;
    private List<ContentTypeMapping> contentTypeMapping;
    private String categoryMappingType;
    private String categoryName;
    private String thirdPartyCategoryType;
    private String skillSpringCategoryType;
    private List<CategoryTypeMapping> categoryTypeMapping;
    private String thirdPartyCompletionStatus;
    private String skillSpringCompletionStatus;
    private List<CompletionSyncMapping> completionSyncMapping;

    // meta data mapping
    private String prefix;
    private List<MetaDataMappings> metaDataMappings;
    private  String thirdPartyMetadataField;
    private String skillSpringMetadataField;

    private List<LessonMetaDataMappings>lessonMetaDataMappings;
    private String thirdPartyLessonMetadataField;
    private String skillSpringLessonMetadataField;


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
    public static class UniqueIdentifiers {
        private String thirdPartyIdentifier;
        private String skillSpringIdentifier;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentTypeMapping{
        private String thirdPartyContentType;
        private String skillSpringContentType;
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
    public static class CompletionSyncMapping {
        private String thirdPartyCompletionStatus;
        private String skillSpringCompletionStatus;
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

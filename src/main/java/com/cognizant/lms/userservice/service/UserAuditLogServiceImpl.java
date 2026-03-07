package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserAuditLogDaoImpl;
import com.cognizant.lms.userservice.dto.UserAuditLogDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class UserAuditLogServiceImpl {
    private static final String awsDynamodbEndpoint = System.getenv("AWS_DYNAMODB_ENDPOINT");
    private static final String regionName = System.getenv("REGION_NAME");

    static DynamoDBConfig dynamoDBConfig = new DynamoDBConfig(awsDynamodbEndpoint, regionName);


    public static void addUserAuditLog(UserAuditLogDto auditLogDto) {
        log.info("Inserting user audit log for user id: {}", auditLogDto.getEmailId());
        UserAuditLogDaoImpl userAuditLogDaoImpl = new UserAuditLogDaoImpl(dynamoDBConfig);
        auditLogDto = userAuditLogDaoImpl.addUserAuditLog(auditLogDto);
        log.info("User audit log inserted successfully for user id: {}", auditLogDto.getEmailId());
    }

    private static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    public static void handleDynamoDBEvent(JsonNode rootNode) {
        log.info("Processing DynamoDB event: {}", rootNode);
        JsonNode recordsNode = rootNode.get("Records");
        if (recordsNode == null || !recordsNode.isArray()) {
            log.warn("No Records found in DynamoDB event");
            return;
        }
        for (JsonNode recordNode : recordsNode) {
            String eventName = recordNode.path("eventName").asText();
            log.info("Event Name: {}", eventName);

            JsonNode dynamodbNode = recordNode.get("dynamodb");
            log.info("DynamoDB Node: {}", dynamodbNode);
            if (dynamodbNode == null) {
                log.warn("No DynamoDB data found for record");
                continue;
            }

            switch (eventName) {
                case "INSERT":
                    JsonNode newImageInsert = dynamodbNode.get("NewImage");
                    log.info("New record inserted: {}", newImageInsert);
                    if (newImageInsert != null) {
                        UserAuditLogDto auditLogDto = new UserAuditLogDto();
                        Map<String, String> insertImageMap = new HashMap<>();
                        newImageInsert.fields().forEachRemaining(entry -> insertImageMap.put(entry.getKey(), entry.getValue().get("S").asText()));

                        auditLogDto.setPk(insertImageMap.get("pk") + "#" +generateUniqueId());
                        auditLogDto.setSk(insertImageMap.get("sk") + "#" + insertImageMap.get("modifiedOn"));
                        auditLogDto.setEmailId(insertImageMap.get("emailId"));
                        String name = insertImageMap.get("firstName") + " " + insertImageMap.get("lastName");
                        auditLogDto.setName(name);
                        auditLogDto.setAction(Constants.ACTION_ADD);
                        auditLogDto.setInstitutionName(insertImageMap.get("institutionName"));
                        auditLogDto.setActionTimestamp(insertImageMap.get("modifiedOn"));
                        auditLogDto.setActionPerformedBy(insertImageMap.get("modifiedBy"));
                        UserAuditLogServiceImpl.addUserAuditLog(auditLogDto);
                    }
                    break;
                case "MODIFY":
                    JsonNode newImage = dynamodbNode.get("NewImage");
                    JsonNode oldImage = dynamodbNode.get("OldImage");
                    log.info("Record Old Image: {} | Record New Image : {}", oldImage, newImage);
                    if (newImage != null && oldImage != null) {
                        log.info("Comparing newImage and oldImage for modifications");
                        Map<String, String> oldImageMap = new HashMap<>();
                        oldImage.fields().forEachRemaining(entry -> oldImageMap.put(entry.getKey(), entry.getValue().get("S").asText()));
                        Map<String, String> modifiedImageMap = new HashMap<>();
                        newImage.fields().forEachRemaining(entry -> modifiedImageMap.put(entry.getKey(), entry.getValue().get("S").asText()));
                        log.info("Old Image Map: {} | New Image Map: {}", oldImageMap, modifiedImageMap);

                        List<String> modifiedFieldsList = new ArrayList<>();
                        modifiedImageMap.forEach((fieldName, newValue) -> {
                            String oldValue = oldImageMap.get(fieldName);
                            if (!Objects.equals(oldValue, newValue)) {
                                String modifiedField = fieldName + "= " + oldValue + " @@ " + newValue;
                                log.info("Modified field: {}", modifiedField);
                                modifiedFieldsList.add(modifiedField);
                            }
                        });
                        String modifiedFieldsForDto = String.join(";", modifiedFieldsList);

                        log.info("Modified Fields: {}", modifiedFieldsForDto);

                        if (!modifiedFieldsForDto.isEmpty()) {
                            log.info("Creating UserAuditLogDto for modified record");
                            UserAuditLogDto auditLogDto = new UserAuditLogDto();
                            auditLogDto.setPk(modifiedImageMap.get("pk") + "#" +generateUniqueId());
                            auditLogDto.setSk(modifiedImageMap.get("sk") + "#" + modifiedImageMap.get("modifiedOn"));
                            auditLogDto.setEmailId(modifiedImageMap.get("emailId"));
                            String name = modifiedImageMap.get("firstName") + " " + modifiedImageMap.get("lastName");
                            auditLogDto.setName(name);
                            auditLogDto.setAction(Constants.ACTION_UPDATE);
                            auditLogDto.setInstitutionName(modifiedImageMap.get("institutionName"));
                            auditLogDto.setActionTimestamp(modifiedImageMap.get("modifiedOn"));
                            auditLogDto.setActionPerformedBy(modifiedImageMap.get("modifiedBy"));
                            auditLogDto.setModifiedFields(modifiedFieldsForDto);
                            log.info("modifiedFieldsForDto" +modifiedFieldsForDto);
                            log.info("Updating user audit log for modified record: {}", auditLogDto);
                            UserAuditLogServiceImpl.addUserAuditLog(auditLogDto);
                        }
                    }
                    break;
                default:
                    log.warn("Unsupported event type: {}", eventName);
            }
        }
    }
}

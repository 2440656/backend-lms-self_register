package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.AddUserEventDetailDto;
import com.cognizant.lms.userservice.dto.SkillMigrationEventDetailDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import java.util.Date;

@Service
@Slf4j
public class UserManagementEventPublisherServiceImpl implements UserManagementEventPublisherService {

  private final String regionName;
  private final String eventSource;
  private final String userManagementEventBusArn;

  private final String eventDetailType;
  private final ObjectMapper objectMapper;

  public UserManagementEventPublisherServiceImpl(@Value("${REGION_NAME}") String regionName,
                                            @Value("${AWS_USER_MANAGEMENT_EVENT_SOURCE}") String eventSource,
                                            @Value("${AWS_USER_MANAGEMENT_EVENT_BUS_ARN}") String userManagementEventBusArn,
                                            @Value("${AWS_USER_MANAGEMENT_EVENT_DETAIL_TYPE}") String eventDetailType) {
    this.regionName = regionName;
    this.eventSource = eventSource;
    this.userManagementEventBusArn = userManagementEventBusArn;

    this.eventDetailType = eventDetailType;
    this.objectMapper = new ObjectMapper();
  }


  @Override
  public void triggerAddUserPublishEvent(AddUserEventDetailDto addUserEventDetailDto) {
    try {
      log.info("Triggering  add user event for fileName: {} ", addUserEventDetailDto.getFileName());
      PutEventsResponse response = sendEvent(addUserEventDetailDto.getFileName(), addUserEventDetailDto.getEventType(),
           addUserEventDetailDto, userManagementEventBusArn);
      handleEventResponse(response, "Add -User (User Management)");
    } catch (Exception e) {
      log.error("Failed to trigger add user event for fileName: {} . Error: {}",
          addUserEventDetailDto.getFileName(), e.getMessage());
      throw new RuntimeException("Error triggering add user event", e);
    }
  }

  @Override
  public void triggerSkillMigrationEvent(SkillMigrationEventDetailDto skillMigrationEventDetailDto) {
    try {
      log.info("Triggering skill migration event for tenantCode: {}",
          skillMigrationEventDetailDto.getTenantCode());
      PutEventsResponse response = sendGenericEvent(skillMigrationEventDetailDto, userManagementEventBusArn);
      handleEventResponse(response, "Skill Migration (User Management)");
    } catch (Exception e) {
      log.error("Failed to trigger skill migration event. Error: {}", e.getMessage());
      throw new RuntimeException("Error triggering skill migration event", e);
    }
  }

  private PutEventsResponse sendGenericEvent(Object detailDto, String targetEventBusArn) {
    try {
      String detail = objectMapper.writeValueAsString(detailDto);
      log.info("Sending skill migration event with detail: {} to event bus: {}", detail, targetEventBusArn);

      try (EventBridgeClient eventBridgeClient = createEventBridgeClient()) {
        PutEventsRequestEntry requestEntry = createRequestEntry(detail, targetEventBusArn);
        PutEventsRequest request = PutEventsRequest.builder()
            .entries(requestEntry)
            .build();

        return eventBridgeClient.putEvents(request);
      }
    } catch (JsonProcessingException e) {
      log.error("Error serializing skill migration event detail: {}", e.getMessage(), e);
      throw new RuntimeException("Error serializing skill migration event detail", e);
    }
  }

  private PutEventsResponse sendEvent(String fileName, String eventType,
                                      AddUserEventDetailDto addUserEventDetailDto,
                                      String targetEventBusArn) throws JsonProcessingException {
    String detail = getEventDetail(eventType, addUserEventDetailDto);
    log.info("Sending {} event with detail: {} to event bus: {}", eventType, detail, targetEventBusArn);

    try (EventBridgeClient eventBridgeClient = createEventBridgeClient()) {
      PutEventsRequestEntry requestEntry = createRequestEntry(detail, targetEventBusArn);
      PutEventsRequest request = PutEventsRequest.builder()
          .entries(requestEntry)
          .build();

      return eventBridgeClient.putEvents(request);
    }
  }

  private String getEventDetail(String eventType, AddUserEventDetailDto addUserEventDetailDto)
      throws JsonProcessingException {
    if (eventType.equalsIgnoreCase(Constants.ADD_USER_EVENT)) {
      return objectMapper.writeValueAsString(addUserEventDetailDto);
    } else {
      throw new IllegalArgumentException("Unsupported event type: " + eventType);
    }
  }

  private EventBridgeClient createEventBridgeClient() {
    return EventBridgeClient.builder()
        .region(Region.of(regionName))
        .build();
  }

  private PutEventsRequestEntry createRequestEntry(String detail, String eventBusArn) {
    return PutEventsRequestEntry.builder()
        .time(new Date().toInstant())
        .source(eventSource)
        .detailType(eventDetailType)
        .eventBusName(eventBusArn)
        .detail(detail)
        .build();
  }

  private void handleEventResponse(PutEventsResponse response, String eventType) {
    if (response.failedEntryCount() > 0) {
      log.error("Failed to trigger {} event for {} . Response: {}", eventType, eventType, response);
      throw new RuntimeException("Failed to trigger " + eventType + " event");
    } else {
      log.info("Successfully triggered {} event for {} . Response: {}", eventType, eventType, response);
    }
  }
}
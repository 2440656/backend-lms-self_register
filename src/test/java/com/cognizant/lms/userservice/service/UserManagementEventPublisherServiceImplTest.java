package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.AddUserEventDetailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementEventPublisherServiceImplTest {

  private UserManagementEventPublisherServiceImpl publisherService;

  private final String eventBusArn = "arn:aws:events:us-east-1:123456789012:event-bus/user-events";

  @BeforeEach
  void setUp() {
    String regionName = "us-east-1";
    String eventSource = "user-service";
    String eventDetailType = "UserEvent";
    publisherService = new UserManagementEventPublisherServiceImpl(
        regionName, eventSource, eventBusArn, eventDetailType
    );
  }

  @Test
  @DisplayName("getEventDetail - success for ADD_USER_EVENT")
  void testGetEventDetail_Success() {
    AddUserEventDetailDto dto = new AddUserEventDetailDto();
    dto.setFileName("users.csv");
    dto.setEventType(Constants.ADD_USER_EVENT);

    assertDoesNotThrow(() -> {
      var method = publisherService
          .getClass()
          .getDeclaredMethod("getEventDetail", String.class, AddUserEventDetailDto.class);
      method.setAccessible(true);

      String detail = (String) method.invoke(publisherService, Constants.ADD_USER_EVENT, dto);
      assertTrue(detail.contains("users.csv"));
    });
  }

  @Test
  @DisplayName("handleEventResponse - success when failedEntryCount is 0")
  void testHandleEventResponse_Success() throws Exception {
    var method = publisherService.getClass()
        .getDeclaredMethod("handleEventResponse", PutEventsResponse.class, String.class);
    method.setAccessible(true);

    PutEventsResponse response = PutEventsResponse.builder()
        .failedEntryCount(0)
        .build();

    assertDoesNotThrow(() -> method.invoke(publisherService, response, "Add -User (User Management)"));
  }

  @Test
  @DisplayName("handleEventResponse - failure when failedEntryCount is greater than 0")
  void testHandleEventResponse_Failure() throws Exception {
    var method = publisherService.getClass()
        .getDeclaredMethod("handleEventResponse", PutEventsResponse.class, String.class);
    method.setAccessible(true);

    PutEventsResponse response = PutEventsResponse.builder()
        .failedEntryCount(1)
        .build();

    InvocationTargetException ex = assertThrows(InvocationTargetException.class, () ->
        method.invoke(publisherService, response, "Add -User (User Management)")
    );

    Throwable cause = ex.getCause();
    assertInstanceOf(RuntimeException.class, cause);
    assertEquals("Failed to trigger Add -User (User Management) event", cause.getMessage());
  }


}

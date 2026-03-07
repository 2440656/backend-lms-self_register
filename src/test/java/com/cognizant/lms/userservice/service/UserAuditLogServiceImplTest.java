package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.UserAuditLogDaoImpl;
import com.cognizant.lms.userservice.dto.UserAuditLogDto;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserAuditLogServiceImplTest {

    private MockedConstruction<UserAuditLogDaoImpl> mocked;

    @BeforeEach
    void setup() {
        mocked = Mockito.mockConstruction(UserAuditLogDaoImpl.class, (mock, context) -> {
            Mockito.when(mock.addUserAuditLog(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        });
    }

    @AfterEach
    void tearDown() {
        mocked.close();
    }

    @Test
    void testUpdateUserAuditLog_Success() {
        UserAuditLogDto dto = new UserAuditLogDto();
        dto.setEmailId("test@example.com");
        assertDoesNotThrow(() -> UserAuditLogServiceImpl.addUserAuditLog(dto));
    }


    @Test
    void testHandleDynamoDBEvent_InsertEvent() {
        ObjectNode newImage = JsonNodeFactory.instance.objectNode();
        newImage.set("pk", JsonNodeFactory.instance.objectNode().put("S", "pk1"));
        newImage.set("sk", JsonNodeFactory.instance.objectNode().put("S", "sk1"));
        newImage.set("emailId", JsonNodeFactory.instance.objectNode().put("S", "test@example.com"));
        newImage.set("firstName", JsonNodeFactory.instance.objectNode().put("S", "John"));
        newImage.set("lastName", JsonNodeFactory.instance.objectNode().put("S", "Doe"));
        newImage.set("modifiedOn", JsonNodeFactory.instance.objectNode().put("S", "2024-06-01T12:00:00Z"));
        newImage.set("modifiedBy", JsonNodeFactory.instance.objectNode().put("S", "admin"));

        ObjectNode dynamodb = JsonNodeFactory.instance.objectNode();
        dynamodb.set("NewImage", newImage);

        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.put("eventName", "INSERT");
        record.set("dynamodb", dynamodb);

        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_ModifyEvent() {
        ObjectNode oldImage = JsonNodeFactory.instance.objectNode();
        oldImage.set("pk", JsonNodeFactory.instance.objectNode().put("S", "pk1"));
        oldImage.set("sk", JsonNodeFactory.instance.objectNode().put("S", "sk1"));
        oldImage.set("emailId", JsonNodeFactory.instance.objectNode().put("S", "test@example.com"));
        oldImage.set("firstName", JsonNodeFactory.instance.objectNode().put("S", "John"));
        oldImage.set("lastName", JsonNodeFactory.instance.objectNode().put("S", "Doe"));
        oldImage.set("modifiedOn", JsonNodeFactory.instance.objectNode().put("S", "2024-06-01T12:00:00Z"));
        oldImage.set("modifiedBy", JsonNodeFactory.instance.objectNode().put("S", "admin"));

        ObjectNode newImage = oldImage.deepCopy();
        newImage.set("firstName", JsonNodeFactory.instance.objectNode().put("S", "Jane")); // changed

        ObjectNode dynamodb = JsonNodeFactory.instance.objectNode();
        dynamodb.set("OldImage", oldImage);
        dynamodb.set("NewImage", newImage);

        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.put("eventName", "MODIFY");
        record.set("dynamodb", dynamodb);

        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_UnsupportedEvent() {
        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.put("eventName", "REMOVE");
        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_NoRecords() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }
    @Test
    void testHandleDynamoDBEvent_RecordWithoutDynamoDBNode() {
        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.put("eventName", "INSERT");
        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_InsertEventWithNullNewImage() {
        ObjectNode dynamodb = JsonNodeFactory.instance.objectNode();
        dynamodb.set("NewImage", NullNode.getInstance());

        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.put("eventName", "INSERT");
        record.set("dynamodb", dynamodb);

        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_ModifyEventWithNullImages() {
        ObjectNode dynamodb = JsonNodeFactory.instance.objectNode();
        dynamodb.set("NewImage", NullNode.getInstance());
        dynamodb.set("OldImage", NullNode.getInstance());

        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.put("eventName", "MODIFY");
        record.set("dynamodb", dynamodb);

        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_RecordWithNullEventName() {
        ObjectNode dynamodb = JsonNodeFactory.instance.objectNode();
        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.set("dynamodb", dynamodb);

        ArrayNode records = JsonNodeFactory.instance.arrayNode();
        records.add(record);

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", records);

        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }

    @Test
    void testHandleDynamoDBEvent_RecordsIsNull() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("Records", NullNode.getInstance());
        assertDoesNotThrow(() -> UserAuditLogServiceImpl.handleDynamoDBEvent(root));
    }
}

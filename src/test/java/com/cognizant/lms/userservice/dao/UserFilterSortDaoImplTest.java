package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UserListResponse;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.exception.UserFilterSortException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFilterSortDaoImplTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @InjectMocks
    private UserFilterSortDaoImpl userFilterSortDao;

    @BeforeEach
    void setUp() {
        TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        userFilterSortDao = new UserFilterSortDaoImpl(dynamoDBConfig, "UserTable", "pk");
    }

//    @Test
//    void testGetUsers() {
//        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
//        lastEvaluatedKey.put("pk", AttributeValue.builder().s("test").build());
//
//        QueryResponse queryResponse = QueryResponse.builder()
//                .count(1)
//                .items(Collections.singletonList(Map.of("pk", AttributeValue.builder().s("test").build())))
//                .lastEvaluatedKey(lastEvaluatedKey)
//                .build();
//
//        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
//
//        UserListResponse response = userFilterSortDao.getUsers("sortKey", "asc", new HashMap<>(), 10, null, null, null, null);
//
//        assertNotNull(response);
//        assertEquals(1, response.getCount());
//        assertFalse(response.getUserList().isEmpty());
//    }

    @Test
    void testGetUsersThrowsException() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.class);

        assertThrows(UserFilterSortException.class, () -> {
            userFilterSortDao.getUsers("sortKey", "asc", new HashMap<>(), 10, null, null, null, null);
        });
    }

    @Test
    void testGetInstitutions() {
        QueryResponse queryResponse = QueryResponse.builder()
                .items(Collections.singletonList(Map.of("institutionName", AttributeValue.builder().s("TestInstitution").build())))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        List<String> institutions = userFilterSortDao.getInstitutions("sortKey");

        assertNotNull(institutions);
        assertFalse(institutions.isEmpty());
        assertEquals("TestInstitution", institutions.get(0));
    }

    @Test
    void testGetUserByEmailId() {
        QueryResponse queryResponse = QueryResponse.builder()
                .items(Collections.singletonList(Map.of("emailId", AttributeValue.builder().s("test@example.com").build())))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        User user = userFilterSortDao.getUserByEmailId("test@example.com", "active");

        assertNotNull(user);
        assertEquals("test@example.com", user.getEmailId());
    }

    @Test
    void testDeactivateUser() {
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        User user = new User();
        user.setPk("testPk");
        user.setSk("testSk");

        boolean result = userFilterSortDao.deactivateUser(user, "admin");

        assertTrue(result);
    }

    @Test
    void testUpdateUser() {
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        User user = new User();
        user.setPk("testPk");
        user.setSk("testSk");

        boolean result = userFilterSortDao.updateUser(user, "admin");

        assertTrue(result);
    }

    @Test
    void testUpdateUserByPk_ShouldReturnTrue_WhenUpdateIsSuccessful() {
        // Arrange
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        User user = new User();
        user.setPk("testPk");
        user.setSk("testSk");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setInstitutionName("Institution");
        user.setUserAccountExpiryDate("12/12/2025");
        user.setRole("learner");
        user.setUserType("Internal");
        user.setStatus("Active");
        user.setCountry("Germany");

        String modifiedBy = "admin";

        // Act
        boolean result = userFilterSortDao.updateUserByPk(user, modifiedBy);

        // Assert
        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateUserByPk_ShouldThrowException_WhenUpdateFails() {
        // Arrange
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenThrow(DynamoDbException.class);

        User user = new User();
        user.setPk("testPk");
        user.setSk("testSk");

        String modifiedBy = "admin";

        // Act & Assert
        assertThrows(DataBaseException.class, () -> {
            userFilterSortDao.updateUserByPk(user, modifiedBy);
        });
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testGetUserByPk_ShouldReturnUser_WhenUserExists() {
        // Arrange
        Map<String, AttributeValue> item = Map.of(
                "pk", AttributeValue.builder().s("testPk").build(),
                "sk", AttributeValue.builder().s("testSk").build(),
                "firstName", AttributeValue.builder().s("John").build(),
                "lastName", AttributeValue.builder().s("Doe").build()
        );
        QueryResponse queryResponse = QueryResponse.builder()
                .items(Collections.singletonList(item))
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        // Act
        User user = userFilterSortDao.getUserByPk("testPk");

        // Assert
        assertNotNull(user);
        assertEquals("testPk", user.getPk());
        assertEquals("testSk", user.getSk());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
    }

    @Test
    void testGetUserByPk_ShouldReturnNull_WhenUserDoesNotExist() {
        // Arrange
        QueryResponse queryResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        // Act
        User user = userFilterSortDao.getUserByPk("nonExistentPk");

        // Assert
        assertNull(user);
    }

    // Test cases for updateUserTermsAccepted method
    @Test
    void testUpdateUserTermsAccepted_Success_WithTermsAcceptedDateNull() {
        // Arrange
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        String pk = "testPk";
        String sk = "testSk";
        String termsAccepted = "Y";
        String termsAcceptedDate = null;

        // Act
        boolean result = userFilterSortDao.updateUserTermsAccepted(pk, sk, termsAccepted, termsAcceptedDate);

        // Assert
        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateUserTermsAccepted_Success_WithTermsAcceptedDate() {
        // Arrange
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        String pk = "testPk";
        String sk = "testSk";
        String termsAccepted = "Y";
        String termsAcceptedDate = "2023-12-01 10:00:00";

        // Act
        boolean result = userFilterSortDao.updateUserTermsAccepted(pk, sk, termsAccepted, termsAcceptedDate);

        // Assert
        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateUserTermsAccepted_Failure_UpdateResponseNotSuccessful() {
        // Arrange
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(400).build())
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        String pk = "testPk";
        String sk = "testSk";
        String termsAccepted = "Y";
        String termsAcceptedDate = "2023-12-01 10:00:00";

        // Act
        boolean result = userFilterSortDao.updateUserTermsAccepted(pk, sk, termsAccepted, termsAcceptedDate);

        // Assert
        assertFalse(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateUserTermsAccepted_Failure_DynamoDbException() {
        // Arrange
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        String pk = "testPk";
        String sk = "testSk";
        String termsAccepted = "Y";
        String termsAcceptedDate = "2023-12-01 10:00:00";

        // Act
        boolean result = userFilterSortDao.updateUserTermsAccepted(pk, sk, termsAccepted, termsAcceptedDate);

        // Assert
        assertFalse(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateUserTermsAccepted_Success_WithTermsAcceptedN() {
        // Arrange
        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        String pk = "testPk";
        String sk = "testSk";
        String termsAccepted = "N";
        String termsAcceptedDate = null;

        // Act
        boolean result = userFilterSortDao.updateUserTermsAccepted(pk, sk, termsAccepted, termsAcceptedDate);

        // Assert
        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    // Test cases for updateTermsAccepted method
    @Test
    void testUpdateTermsAccepted_Success_WithExternalUsers() {
        // Arrange
        Map<String, AttributeValue> externalUser1 = Map.of(
                "pk", AttributeValue.builder().s("user1").build(),
                "sk", AttributeValue.builder().s("sk1").build(),
                "userType", AttributeValue.builder().s("External").build()
        );
        Map<String, AttributeValue> externalUser2 = Map.of(
                "pk", AttributeValue.builder().s("user2").build(),
                "sk", AttributeValue.builder().s("sk2").build(),
                "userType", AttributeValue.builder().s("External").build()
        );

        QueryResponse queryResponse = QueryResponse.builder()
                .items(List.of(externalUser1, externalUser2))
                .build();

        UpdateItemResponse updateItemResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        // Act
        boolean result = userFilterSortDao.updateTermsAccepted();

        // Assert
        assertTrue(result);
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
        verify(dynamoDbClient, times(2)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateTermsAccepted_Success_WithNoExternalUsers() {
        // Arrange
        QueryResponse queryResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        // Act
        boolean result = userFilterSortDao.updateTermsAccepted();

        // Assert
        assertTrue(result);
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
        verify(dynamoDbClient, never()).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateTermsAccepted_Failure_QueryThrowsException() {
        // Arrange
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Query failed").build());

        // Act
        boolean result = userFilterSortDao.updateTermsAccepted();

        // Assert
        assertFalse(result);
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
        verify(dynamoDbClient, never()).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateTermsAccepted_PartialFailure_SomeUpdatesSucceed() {
        // Arrange
        Map<String, AttributeValue> externalUser1 = Map.of(
                "pk", AttributeValue.builder().s("user1").build(),
                "sk", AttributeValue.builder().s("sk1").build(),
                "userType", AttributeValue.builder().s("External").build()
        );
        Map<String, AttributeValue> externalUser2 = Map.of(
                "pk", AttributeValue.builder().s("user2").build(),
                "sk", AttributeValue.builder().s("sk2").build(),
                "userType", AttributeValue.builder().s("External").build()
        );

        QueryResponse queryResponse = QueryResponse.builder()
                .items(List.of(externalUser1, externalUser2))
                .build();

        UpdateItemResponse successResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        UpdateItemResponse failureResponse = (UpdateItemResponse) UpdateItemResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(400).build())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(successResponse)
                .thenReturn(failureResponse);

        // Act
        boolean result = userFilterSortDao.updateTermsAccepted();

        // Assert
        assertFalse(result);
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
        verify(dynamoDbClient, times(2)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateTermsAccepted_Failure_UpdateThrowsException() {
        // Arrange
        Map<String, AttributeValue> externalUser1 = Map.of(
                "pk", AttributeValue.builder().s("user1").build(),
                "sk", AttributeValue.builder().s("sk1").build(),
                "userType", AttributeValue.builder().s("External").build()
        );

        QueryResponse queryResponse = QueryResponse.builder()
                .items(List.of(externalUser1))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Update failed").build());

        // Act
        boolean result = userFilterSortDao.updateTermsAccepted();

        // Assert
        assertFalse(result);
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testGetUsersReturnsEmptyList() {
        QueryResponse queryResponse = QueryResponse.builder()
            .count(0)
            .items(Collections.emptyList())
            .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        UserListResponse response = userFilterSortDao.getUsers("sortKey", "asc", new HashMap<>(), 10, null, null, null, null);

        assertNotNull(response);
        assertEquals(0, response.getCount());
        assertTrue(response.getUserList().isEmpty());
    }

    @Test
    void testGetUsersWithNullSortKey() {
        QueryResponse queryResponse = QueryResponse.builder()
            .count(1)
            .items(Collections.singletonList(Map.of("pk", AttributeValue.builder().s("test").build())))
            .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        UserListResponse response = userFilterSortDao.getUsers(null, "asc", new HashMap<>(), 10, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getCount());
        assertFalse(response.getUserList().isEmpty());
    }



    @Test
    void testGetUsersWithNullFilters() {
        QueryResponse queryResponse = QueryResponse.builder()
            .count(1)
            .items(Collections.singletonList(Map.of("pk", AttributeValue.builder().s("test").build())))
            .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        UserListResponse response = userFilterSortDao.getUsers("sortKey", "asc", null, 10, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getCount());
        assertFalse(response.getUserList().isEmpty());
    }

    @Test
    void testGetInstitutionsReturnsEmptyList() {
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.emptyList())
            .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        List<String> institutions = userFilterSortDao.getInstitutions("sortKey");

        assertNotNull(institutions);
        assertTrue(institutions.isEmpty());
    }
    @Test
    void testGetInstitutionsWithNullSortKey() {
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.singletonList(Map.of("institutionName", AttributeValue.builder().s("TestInstitution").build())))
            .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        List<String> institutions = userFilterSortDao.getInstitutions(null);

        assertNotNull(institutions);
        assertFalse(institutions.isEmpty());
        assertEquals("TestInstitution", institutions.get(0));
    }

    // Add to UserFilterSortDaoImplTest.java

    @Test
    void testUpdateLearnerPreferredView_Success() {
        User user = new User();
        user.setPk("pk1");
        user.setSk("sk1");
        String preferredUI = "DARK";

        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(200).build());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        boolean result = userFilterSortDao.updateLearnerPreferredView(user, preferredUI);

        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateLearnerPreferredView_Failure_UnsuccessfulResponse() {
        User user = new User();
        user.setPk("pk2");
        user.setSk("sk2");
        String preferredUI = "LIGHT";

        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(500).build());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        DataBaseException ex = assertThrows(DataBaseException.class, () ->
            userFilterSortDao.updateLearnerPreferredView(user, preferredUI)
        );
        assertTrue(ex.getMessage().contains("Failed to update preferredUI"));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateLearnerPreferredView_Failure_DynamoDbException() {
        User user = new User();
        user.setPk("pk3");
        user.setSk("sk3");
        String preferredUI = "DEFAULT";

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenThrow(DynamoDbException.builder().message("Dynamo error").build());

        DataBaseException ex = assertThrows(DataBaseException.class, () ->
            userFilterSortDao.updateLearnerPreferredView(user, preferredUI)
        );
        assertTrue(ex.getMessage().contains("Error updating preferredUI"));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    // ===== Tests for getUserByTenantCode =====

    @Test
    void testGetUserByTenantCode_Success_buildsExpectedQuery_andMapsResponse() {
        // Arrange
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s("t-2").build());
        item.put("sk", AttributeValue.builder().s("user#u1").build());
        item.put("emailId", AttributeValue.builder().s("user1@example.com").build());
        item.put("firstName", AttributeValue.builder().s("Jane").build());
        item.put("lastName", AttributeValue.builder().s("Doe").build());
        item.put("role", AttributeValue.builder().s("learner").build());
        item.put("loginOption", AttributeValue.builder().s("LOCAL").build());
        item.put("status", AttributeValue.builder().s("Active").build());

        QueryResponse queryResponse = QueryResponse.builder()
                .items(List.of(item))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        // Act
        List<User> result = userFilterSortDao.getUserByTenantCode("t-2");

        // Assert: returned users are mapped
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user1@example.com", result.get(0).getEmailId());

        // Assert: correct QueryRequest built
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dynamoDbClient, times(1)).query(requestCaptor.capture());
        QueryRequest sent = requestCaptor.getValue();

        assertEquals("UserTable", sent.tableName());
        assertEquals("gsi_sort_createdOn", sent.indexName());
        assertEquals("pk = :PK", sent.keyConditionExpression());
        assertNotNull(sent.filterExpression());
        assertTrue(sent.filterExpression().contains("#status = :status"));
        assertNotNull(sent.expressionAttributeValues());
        assertEquals("t-2", sent.expressionAttributeValues().get(":PK").s());
    }

    @Test
    void testGetUserByTenantCode_EmptyResult_returnsEmptyList() {
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(Collections.emptyList()).build());

        List<User> result = userFilterSortDao.getUserByTenantCode("t-2");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
    }

    @Test
    void testGetUserByTenantCode_DynamoDbException_throwsDataBaseException() {
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("boom").build());

        assertThrows(DataBaseException.class, () -> userFilterSortDao.getUserByTenantCode("t-2"));
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
    }

    @Test
    void testUpdateIsWatchedTutorial_Success() {
        String pk = "testPk";
        String sk = "testSk";

        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(200).build());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        boolean result = userFilterSortDao.updateIsWatchedTutorial(pk, sk);

        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateIsWatchedTutorial_Failure_UnsuccessfulResponse() {
        String pk = "testPk";
        String sk = "testSk";

        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(500).build());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        boolean result = userFilterSortDao.updateIsWatchedTutorial(pk, sk);

        assertFalse(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateIsWatchedTutorial_Failure_DynamoDbException() {
        String pk = "testPk";
        String sk = "testSk";

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Dynamo error").build());

        boolean result = userFilterSortDao.updateIsWatchedTutorial(pk, sk);

        assertFalse(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateVideoLaunchCount_Success() {
        String pk = "testPk";
        String sk = "testSk";

        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(200).build());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        boolean result = userFilterSortDao.updateVideoLaunchCount(pk, sk);

        assertTrue(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateVideoLaunchCount_Failure_UnsuccessfulResponse() {
        String pk = "testPk";
        String sk = "testSk";

        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.sdkHttpResponse()).thenReturn(SdkHttpResponse.builder().statusCode(500).build());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);

        boolean result = userFilterSortDao.updateVideoLaunchCount(pk, sk);

        assertFalse(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateVideoLaunchCount_Failure_DynamoDbException() {
        String pk = "testPk";
        String sk = "testSk";

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Dynamo error").build());
        boolean result = userFilterSortDao.updateVideoLaunchCount(pk, sk);
        assertFalse(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

}


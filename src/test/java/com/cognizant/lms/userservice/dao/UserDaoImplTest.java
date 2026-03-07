package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.dto.UpdateDateDTO;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDaoImplTest {

    @Mock
    private DynamoDBConfig dynamoDBConfig;
    @Mock
    private DynamoDbClient dynamoDbClient;
    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    @Mock
    private DynamoDbTable<User> userTable;
    private UserDaoImpl userDao;

    @Mock
    private UserFilterSortDao userFilterSortDao;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDBConfig.getDynamoDBEnhancedClient()).thenReturn(dynamoDbEnhancedClient);
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        when(dynamoDbEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(userTable);

        userDao = new UserDaoImpl(dynamoDBConfig, "test", "tenants");

    }

    @Test
    public void testCreateUser() {
        User user = new User();
        userDao.createUser(user);
        verify(userTable, times(1)).putItem(user);
    }

    @Test
    public void testUserExists_UserFound() {
        String emailId = "user@example.com";
        User user = new User();
        user.setEmailId(emailId);

        Page<User> page = mock(Page.class);
        SdkIterable<User> mockedSdkIterable = mock(SdkIterable.class);
        when(mockedSdkIterable.stream()).thenReturn(Stream.of(user));
        PageIterable<User> pageIterable = mock(PageIterable.class);
        when(pageIterable.items()).thenReturn(mockedSdkIterable);

        when(userTable.scan()).thenReturn(pageIterable);

        boolean exists = userDao.userExists(emailId);
        assertTrue(exists);
    }

    @Test
    public void testUserExists_UserNotFound() {
        String emailId = "user@example.com";

        Page<User> page = mock(Page.class);
        SdkIterable<User> mockedSdkIterable = mock(SdkIterable.class);
        when(mockedSdkIterable.stream()).thenReturn(Stream.empty());
        PageIterable<User> pageIterable = mock(PageIterable.class);
        when(pageIterable.items()).thenReturn(mockedSdkIterable);

        when(userTable.scan()).thenReturn(pageIterable);

        boolean exists = userDao.userExists(emailId);
        assertFalse(exists);
    }



    @Test
    void testGetAllUsers_ReturnsEmptyList() {
        // Arrange
        PageIterable<User> pageIterable = mock(PageIterable.class);
        SdkIterable<User> mockedSdkIterable = mock(SdkIterable.class);
        lenient().when(mockedSdkIterable.stream()).thenReturn(Stream.empty());
        lenient().when(pageIterable.items()).thenReturn(mockedSdkIterable);
        when(userTable.scan()).thenReturn(pageIterable);

        // Act
        List<User> users = userDao.getAllUsers();

        // Assert
        assertNotNull(users);
        assertTrue(users.isEmpty());
        verify(userTable, times(1)).scan();
    }

    @Test
    void testUpdateUser_Success() {

        User user = new User();
        user.setEmailId("user@example.com");

        userDao.createUser(user);

        verify(userTable, times(1)).putItem(user);
    }


    @Test
    void testUpdateLastLoginTimeStampAndPasswordChangedDate_Success() {
        // Arrange
        String pk = "user#123";
        String sk = "metadata";
        UpdateDateDTO updateDateDTO = new UpdateDateDTO();
        updateDateDTO.setEmailId("user@example.com");
        updateDateDTO.setLastLoginTimestamp("2023-10-01T10:00:00Z");
        updateDateDTO.setPasswordChangedDate("2023-10-01T10:00:00Z");

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        // Act
        userDao.updateLastLoginTimeStampAndPasswordChangedDate(pk, sk, updateDateDTO);

        // Assert
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testUpdateLastLoginTimeStampAndPasswordChangedDate_Failure() {
        // Arrange
        String pk = "user#123";
        String sk = "metadata";
        UpdateDateDTO updateDateDTO = new UpdateDateDTO();
        updateDateDTO.setEmailId("user@example.com");
        updateDateDTO.setLastLoginTimestamp("2023-10-01T10:00:00Z");
        updateDateDTO.setPasswordChangedDate("2023-10-01T10:00:00Z");

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDao.updateLastLoginTimeStampAndPasswordChangedDate(pk, sk, updateDateDTO);
        });
        assertEquals("Failed to update user in DynamoDB", exception.getMessage());
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testAddPasswordChangedDate_Success() {
        // Arrange
        String pk = "user#123";
        String sk = "metadata";
        String emailId = "user@example.com";
        String passwordChangedDate = "2023-10-01T10:00:00Z";

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        // Act
        userDao.addPasswordChangedDate(pk, sk, emailId, passwordChangedDate);

        // Assert
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testAddPasswordChangedDate_Failure() {
        // Arrange
        String pk = "user#123";
        String sk = "metadata";
        String emailId = "user@example.com";
        String passwordChangedDate = "2023-10-01T10:00:00Z";

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDao.addPasswordChangedDate(pk, sk, emailId, passwordChangedDate);
        });
        assertEquals("Failed to add passwordChangedDate in DynamoDB", exception.getMessage());
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }


}
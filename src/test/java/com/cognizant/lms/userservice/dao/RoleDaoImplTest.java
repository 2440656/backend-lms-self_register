package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.utils.LogUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleDaoImplTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @InjectMocks
    private RoleDaoImpl roleDaoImpl;

    @BeforeEach
    void setUp() {
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        roleDaoImpl = new RoleDaoImpl(dynamoDBConfig, "RolesTable", "pk");
    }

    @Test
    void testGetRoles_Success() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.items()).thenReturn(List.of(
                Map.of("pk", AttributeValue.builder().s("Role").build(),
                        "name", AttributeValue.builder().s("Admin").build())
        ));
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        List<RoleDto> roles = roleDaoImpl.getRoles();

        assertEquals(1, roles.size());
        assertEquals("Admin", roles.get(0).getName());
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
    }
}








 
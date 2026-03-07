package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.OperationHistoryResponse;
import com.cognizant.lms.userservice.service.OperationsHistoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;


public class OperationsHistoryControllerTest {

    @Mock
    private OperationsHistoryService operationsHistoryService;


    @InjectMocks
    private OperationsHistoryController operationsHistoryController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLogFiles_success() {
        OperationHistoryResponse mockResponse = new OperationHistoryResponse();
        mockResponse.setStatus(HttpStatus.OK.value());
        when(operationsHistoryService.getLogFiles(anyString(), anyString(), any(), anyString(), any(), anyInt()))
                .thenReturn(mockResponse);

        ResponseEntity<OperationHistoryResponse> response = operationsHistoryController.getAllLogFiles(
            "createdOn", "desc", 5, null,"user-management", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void getAllLogFiles_badRequest() {
        OperationHistoryResponse mockResponse = new OperationHistoryResponse();
        mockResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        mockResponse.setError("Bad Request");
        when(operationsHistoryService.getLogFiles(anyString(), anyString(), any(), anyString(), any(), anyInt()))
                .thenReturn(mockResponse);

        ResponseEntity<OperationHistoryResponse> response = operationsHistoryController.getAllLogFiles(
            "createdOn", "desc", 5, null,"user-management", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

}
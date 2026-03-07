package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.MessageRequest;
import com.cognizant.lms.userservice.dto.MessageResponse;
import com.cognizant.lms.userservice.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @InjectMocks
    private MessageController messageController;

    @Mock
    private MessageService messageService;

    private MessageRequest validRequest;
    private MessageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new MessageRequest();
        validRequest.setCategory("Alert");
        validRequest.setMessage("This is a valid alert message.");

        sampleResponse = new MessageResponse();
        sampleResponse.setMessage("This is a valid alert message.");
        sampleResponse.setCategory("Alert");
        sampleResponse.setStatus(true);
    }

    @Test
    void testPublishMessage_Success() {
        ResponseEntity<HttpResponse> response = messageController.publishMessage(validRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Message published successfully", response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void testPublishMessage_MessageTooLong() {
        validRequest.setMessage("A".repeat(501));
        ResponseEntity<HttpResponse> response = messageController.publishMessage(validRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Message cannot exceed 500 characters.", response.getBody().getError());
    }

    @Test
    void testPublishMessage_Exception() {
        doThrow(new RuntimeException("Service error")).when(messageService).publishMessage(any());
        ResponseEntity<HttpResponse> response = messageController.publishMessage(validRequest);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getError().contains("Service error"));
    }

    @Test
    void testMessageByCategoryAndStatus_Success_WithStatusTrue() {
        String category = "Alert";
        Boolean status = true;
        List<MessageResponse> responseList = List.of(sampleResponse);

        when(messageService.getMessageByCategoryAndStatus(category, status)).thenReturn(responseList);

        ResponseEntity<HttpResponse> response = messageController.messageByCategoryAndStatus(category, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseList, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void testMessageByCategoryAndStatus_EmptyList() {
        String category = "Alert";
        Boolean status = true;

        when(messageService.getMessageByCategoryAndStatus(category, status)).thenReturn(Collections.emptyList());

        ResponseEntity<HttpResponse> response = messageController.messageByCategoryAndStatus(category, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No messages found for the given parameters", response.getBody().getError());
        assertNull(response.getBody().getData());
    }

    @Test
    void testMessageByCategoryAndStatus_NullResponseFromService() {
        String category = "Alert";
        Boolean status = false;

        when(messageService.getMessageByCategoryAndStatus(category, status)).thenReturn(null);

        ResponseEntity<HttpResponse> response = messageController.messageByCategoryAndStatus(category, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No messages found for the given parameters", response.getBody().getError());
        assertNull(response.getBody().getData());
    }

    @Test
    void testMessageByCategoryAndStatus_ExceptionThrown() {
        String category = "Alert";
        Boolean status = true;

        when(messageService.getMessageByCategoryAndStatus(category, status))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<HttpResponse> response = messageController.messageByCategoryAndStatus(category, status);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getError().contains("Failed to retrieve messages: Database error"));
        assertNull(response.getBody().getData());
    }
}



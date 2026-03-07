package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.MessageRequest;
import com.cognizant.lms.userservice.dto.MessageResponse;
import com.cognizant.lms.userservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/message")
@Slf4j
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping()
    @PreAuthorize("hasAnyRole('system-admin','super-admin')")
    public ResponseEntity<HttpResponse> publishMessage(@RequestBody MessageRequest messageRequest) {
        HttpResponse response = new HttpResponse();
        try {
            if (messageRequest.getMessage().length() > 500) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("Message cannot exceed 500 characters.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            messageService.publishMessage(messageRequest);
            response.setData("Message published successfully");
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to publish message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','learner','content-author','mentor','catalog-admin','operations')")
    public ResponseEntity<HttpResponse> messageByCategoryAndStatus(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) Boolean status) {
        HttpResponse response = new HttpResponse();
        try {
            List<MessageResponse> messageResponses = messageService.getMessageByCategoryAndStatus(category, status);
            if (messageResponses == null || messageResponses.isEmpty()) {
                response.setStatus(HttpStatus.OK.value());
                response.setError("No messages found for the given parameters");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            response.setData(messageResponses);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to retrieve messages: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
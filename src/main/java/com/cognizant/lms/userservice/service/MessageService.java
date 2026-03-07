package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.MessageRequest;
import com.cognizant.lms.userservice.dto.MessageResponse;

import java.util.List;

public interface MessageService {
    void publishMessage(MessageRequest messageRequest);
    List<MessageResponse> getMessageByCategoryAndStatus(String category, Boolean status);

}

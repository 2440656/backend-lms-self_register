package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;

import java.util.List;

public interface LookupService {

    /**
     * method to get lookup data based on type and optional skSuffix
     * @return List of LookupDto
     */
    List<LookupDto> getLookupsList(String type, String skSuffix);

    List<LookupDto> getServiceLineLookupsList(String type, String skSuffix);

    List<AiVoicePreviewLookupDto> generateAiVoicePreviewUrls();
}


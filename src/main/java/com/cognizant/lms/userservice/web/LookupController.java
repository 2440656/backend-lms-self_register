package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.service.LookupService;
import com.cognizant.lms.userservice.utils.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/lookups")
@Slf4j
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','operations','learner-delivery-admin','content-author','async-facilitator')")
    public ResponseEntity<HttpResponse> getLookups(@RequestParam String type,
                                                   @RequestParam(required = false) String skSuffix) {
        log.info(LogUtil.getLogInfo("GET_LOOKUP",
                ProcessConstants.IN_PROGRESS) + "Fetching all lookups for type: {}, skSuffix: {} ", type, skSuffix);
        HttpResponse response = new HttpResponse();
        List<LookupDto> lookupData = lookupService.getLookupsList(type, skSuffix);
        response.setData(lookupData);
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        log.info(LogUtil.getLogInfo("GET_LOOKUP",
                ProcessConstants.COMPLETED) + "Fetched lookup data: {} ", response.getData());
        return ResponseEntity.ok(response);

    }

    @GetMapping("/service-line")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','operations','learner-delivery-admin','content-author','async-facilitator')")
    public ResponseEntity<HttpResponse> getServiceLineLookups(@RequestParam String type,
                                                   @RequestParam(required = false) String skSuffix) {
        HttpResponse response = new HttpResponse();
        List<LookupDto> lookupData = lookupService.getServiceLineLookupsList(type, skSuffix);
        response.setData(lookupData);
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        log.info(LogUtil.getLogInfo("GET_LOOKUP",
                ProcessConstants.COMPLETED) + "Fetched lookup data: {} ", response.getData());
        return ResponseEntity.ok(response);

    }

    @GetMapping("/ai-voice-preview-url")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','operations','learner-delivery-admin','content-author','async-facilitator')")
    public ResponseEntity<HttpResponse> getAiVoicePreviewUrl() {
        log.info(LogUtil.getLogInfo("GET_AI_VOICE_PREVIEW_URLS", ProcessConstants.IN_PROGRESS) + "Generating AI voice preview URLs");
        List<AiVoicePreviewLookupDto> aiVoicePreviewResponse = lookupService.generateAiVoicePreviewUrls();
        HttpResponse response = new HttpResponse();
        response.setData(aiVoicePreviewResponse);
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        log.info(LogUtil.getLogInfo("GET_AI_VOICE_PREVIEW_URLS", ProcessConstants.COMPLETED) + "Generated URLs: {}", aiVoicePreviewResponse);
        return ResponseEntity.ok(response);
    }
}

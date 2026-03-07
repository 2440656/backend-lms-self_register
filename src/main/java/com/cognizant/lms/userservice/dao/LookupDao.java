package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupDao {

    List<LookupDto> getLookupData(String type, String skSuffix);

    List<LookupDto> getServiceLineLookupData(String type, String skSuffix);

    List<AiVoicePreviewLookupDto> getAiVoicePreviewData();
}

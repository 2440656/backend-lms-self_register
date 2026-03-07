package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.PopularLinkDto;
import com.cognizant.lms.userservice.dto.PopularLinkRequestDto;
import com.cognizant.lms.userservice.dto.PopularLinksResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PopularLinkService {
    PopularLinkDto savePopularLink(PopularLinkRequestDto linkRequest, MultipartFile file);

    PopularLinksResponse getAllPopularLinks();

    PopularLinkDto updatePopularLink(PopularLinkRequestDto updateLinkRequest, MultipartFile file, 
    boolean keepExistingFile, boolean removeExistingFile);

    String deletePopularLink(String linkId);

    PopularLinksResponse reorderPopularLink(PopularLinkRequestDto reorderRequest);
}
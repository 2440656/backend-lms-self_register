package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.QuickLinkDto;
import com.cognizant.lms.userservice.dto.QuickLinkRequestDto;
import com.cognizant.lms.userservice.dto.QuickLinksResponse;

public interface QuickLinkService {
    QuickLinkDto saveQuickLink(QuickLinkRequestDto linkRequest);

    QuickLinksResponse getAllQuickLinks();

    QuickLinkDto updateQuickLink(QuickLinkRequestDto updateLinkRequest);

    String deleteQuickLink(String linkId);

    QuickLinksResponse reorderQuickLink(QuickLinkRequestDto reorderRequest);
}

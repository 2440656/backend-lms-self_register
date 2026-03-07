package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickLinksResponse {
    private List<QuickLinkDto> quickLinks;
    private int count;

    public void setQuickLinks(List<QuickLinkDto> links) {
        this.quickLinks = links;
    }

    public List<QuickLinkDto> getQuickLinks() {
        return this.quickLinks;
    }
}
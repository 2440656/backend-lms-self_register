package com.cognizant.lms.userservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class BannerManagementDto {
    private String bannerId;
    private String bannerTitle;
    private String bannerDescription;
    private String bannerStatus;
    private String startDate;
    private String endDate;
    private String bannerHeading;
    private String bannerSubHeading;
    private String bannerRedirectionUrl;
    private String bannerImageKey;
}

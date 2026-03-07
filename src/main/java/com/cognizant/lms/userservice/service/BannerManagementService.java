package com.cognizant.lms.userservice.service;


import com.cognizant.lms.userservice.dto.BannerManagementDto;
import com.cognizant.lms.userservice.dto.BannerManagementResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BannerManagementService {

    BannerManagementDto saveBannerManagementIcon(BannerManagementDto bannerManagementDto,MultipartFile file);

    BannerManagementResponse getAllBanners();

    void deleteBanner(String bannerId);
}

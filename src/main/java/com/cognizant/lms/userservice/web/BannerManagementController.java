package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.BannerManagementDto;
import com.cognizant.lms.userservice.dto.BannerManagementResponse;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.exception.BannerManagementActiveLimitException;
import com.cognizant.lms.userservice.service.BannerManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("api/v1/bannerManagement")
public class BannerManagementController {

    private final BannerManagementService bannerManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BannerManagementController(BannerManagementService bannerManagementService) {
        this.bannerManagementService = bannerManagementService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> saveBannerManagementIcon(
            @RequestPart("bannerRequest") String bannerDetailsJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Saving banner management Data");
        HttpResponse response = new HttpResponse();
        BannerManagementDto bannerDetails;
        try {
            bannerDetails = objectMapper.readValue(bannerDetailsJson, BannerManagementDto.class);
        } catch (Exception e) {
            log.error("Error parsing banner details JSON: {}", e.getMessage());
            response.setStatus(400);
            response.setError("Invalid banner details format");
            return ResponseEntity.badRequest().body(response);
        }
        if (bannerDetails.getBannerTitle() == null || bannerDetails.getBannerDescription() == null ||
                bannerDetails.getStartDate() == null || bannerDetails.getEndDate() == null ||
                bannerDetails.getBannerHeading() == null || bannerDetails.getBannerSubHeading() == null ||
                bannerDetails.getBannerRedirectionUrl() == null) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Mandatory fields are missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        try {
            BannerManagementDto bannerManagementDto = bannerManagementService.saveBannerManagementIcon(bannerDetails, file);
            response.setData(bannerManagementDto);
            response.setStatus(HttpStatus.CREATED.value());
            response.setError(null);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (BannerManagementActiveLimitException e) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Failed to save banner management: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("An error occurred while saving banner details" + (e.getMessage() != null ? e.getMessage() : "internal error"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin','learner','content-author','mentor')")
    public ResponseEntity<HttpResponse> getAllBanners() {
        HttpResponse response = new HttpResponse();
        try {
            BannerManagementResponse bannerManagementResponse = bannerManagementService.getAllBanners();
            response.setData(bannerManagementResponse);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching Banners: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to fetch Banners");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{bannerId}")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin','learner','content-author','mentor')")
    public ResponseEntity<HttpResponse> deleteBanner(@PathVariable String bannerId) {
        HttpResponse response = new HttpResponse();
        try {
            bannerManagementService.deleteBanner(bannerId);
            response.setData("Banner deleted successfully");
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting banner: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to delete banner");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

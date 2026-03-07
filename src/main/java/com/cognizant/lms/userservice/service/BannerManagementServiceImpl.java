package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.BannerManagementDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.BannerManagementDto;
import com.cognizant.lms.userservice.dto.BannerManagementResponse;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
public class BannerManagementServiceImpl implements BannerManagementService {

    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB in bytes
    private static final int REQUIRED_WIDTH = 500;
    private static final int REQUIRED_HEIGHT = 500;
    private static final int MAX_ACTIVE_BANNERS = 5;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private final BannerManagementDao bannerManagementDao;
    private final S3Client s3ThumbnailClient;

    @Value("${AWS_ICONS_S3_BUCKET_NAME}")
    private String bucketName;

    @Autowired
    public BannerManagementServiceImpl(BannerManagementDao bannerManagementDao, S3Client s3ThumbnailClient) {
        this.bannerManagementDao = bannerManagementDao;
        this.s3ThumbnailClient = s3ThumbnailClient;
    }

    private BannerManagementDto toDto(Tenant tenant) {
        if (tenant == null) return null;
        return new BannerManagementDto(
                tenant.getBannerId(),
                tenant.getBannerTitle(),
                tenant.getBannerDescription(),
                tenant.getBannerStatus(),
                tenant.getStartDate(),
                tenant.getEndDate(),
                tenant.getBannerHeading(),
                tenant.getBannerSubHeading(),
                tenant.getBannerRedirectionUrl(),
                tenant.getBannerImageKey()
        );
    }

    private Tenant toEntity(BannerManagementDto dto) {
        Tenant entity = new Tenant();
        entity.setBannerId(dto.getBannerId());
        entity.setBannerTitle(dto.getBannerTitle());
        String tenantCode = TenantUtil.getTenantCode();
        entity.setPk(tenantCode);
        String bannerId = dto.getBannerId() != null ? dto.getBannerId() : "";
        String cleanBannerId = bannerId.startsWith("BANNER#") ? bannerId.substring(7) : bannerId;
        entity.setSk("BANNER#" + cleanBannerId);
        entity.setBannerDescription(dto.getBannerDescription());
        entity.setBannerStatus(dto.getBannerStatus());
        entity.setStartDate(dto.getStartDate());
        entity.setBannerHeading(dto.getBannerHeading());
        entity.setBannerSubHeading(dto.getBannerSubHeading());
        entity.setBannerRedirectionUrl(dto.getBannerRedirectionUrl());
        entity.setBannerImageKey(dto.getBannerImageKey());
        return entity;
    }

    @Override
    public BannerManagementDto saveBannerManagementIcon(BannerManagementDto bannerManagementDto, MultipartFile file) {
        Tenant tenant= toEntity(bannerManagementDto);
        log.info("Inside saveBannerManagementDetails of BannerManagementServiceImpl");
        boolean isNewBanner = bannerManagementDto.getBannerId() == null || bannerManagementDto.getBannerId().isEmpty();
        if(isNewBanner){
            log.info("Generating new Banner ID");
            String newBannerId = "BANNER#" + UUID.randomUUID();
            tenant.setBannerId(newBannerId);
            bannerManagementDto.setBannerId(newBannerId);
            String cleanBannerId = newBannerId.startsWith("BANNER#") ? newBannerId.substring(7) : newBannerId;
            tenant.setSk("BANNER#" + cleanBannerId);
            if ("ACTIVE".equals(bannerManagementDto.getBannerStatus())) {
                String tenantCode = TenantUtil.getTenantCode();
                int activeBannerCount = bannerManagementDao.countActiveBanners(tenantCode);
                if (activeBannerCount >= MAX_ACTIVE_BANNERS) {
                    tenant.setBannerStatus("DRAFT");
                }
            }
            else{
                tenant.setBannerStatus("DRAFT");
            }
        }
        String username = UserContext.getCreatedBy();
        tenant.setCreatedBy(username);
        tenant.setCreatedOn(Instant.now().toString());
        if (bannerManagementDto.getEndDate() != null && !bannerManagementDto.getEndDate().toString().trim().isEmpty()) {
            tenant.setEndDate(bannerManagementDto.getEndDate());
            log.info("End date provided: {}", bannerManagementDto.getEndDate());
        } else {
            tenant.setEndDate(null);
            log.info("End date not provided - banner will have no expiration");
        }
        if(file !=null && !file.isEmpty()) {
            validateFile(file);
            String key = "banners/image/" + tenant.getPk() + "/" + tenant.getSk() + "-" + Optional.ofNullable(file.getOriginalFilename()).orElse("icon");
            try {
                byte[] bytes = file.getBytes();
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))
                        .build();
                s3ThumbnailClient.putObject(putReq, RequestBody.fromBytes(bytes));
                tenant.setBannerImageKey(key);
                bannerManagementDto.setBannerImageKey(key);
            } catch (IOException e) {
                log.error("Error uploading file to S3: {}", e.getMessage());
                throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
            }
        }
        bannerManagementDao.saveBannerManagementIcon(tenant);
        return toDto(tenant);
    }

    @Override
    public BannerManagementResponse getAllBanners() {
        String tenantCode = TenantUtil.getTenantCode();
        log.info("Inside get all banners serviceimpl for tenant: {}", tenantCode);
        List<Tenant> bannerList = bannerManagementDao.getBannersByTenant(tenantCode);
        log.info("Fetched {} banners for tenant: {}", bannerList.size(), bannerList);
        List<BannerManagementDto> dtoList = bannerList.stream()
                .map(link -> toDto(link))
                .toList();
        BannerManagementResponse response = new BannerManagementResponse();
        response.setBannerManagementList(dtoList);
        response.setTotalActiveRecords(dtoList.size());
        return response;
    }

    @Override
    public void deleteBanner(String bannerId) {
        String tenantCode = TenantUtil.getTenantCode();
        bannerManagementDao.deleteBannerById(tenantCode, bannerId);
        log.info("Banner deleted for tenantCode: {}, bannerId: {}", tenantCode, bannerId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            double sizeMb = file.getSize() / (1024.0 * 1024.0);
            throw new IllegalArgumentException("File size must be less than or equal to 1MB. Current size: "
                + String.format("%.2f", sizeMb) + "MB");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only PNG, JPG, and JPEG files are allowed. Received: "
                + contentType);
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("Invalid image file");
            }
            int width = image.getWidth();
            int height = image.getHeight();

            if (width < REQUIRED_WIDTH || height < REQUIRED_HEIGHT) {
                throw new IllegalArgumentException(
                    String.format("Image dimensions must be at least %dx%d pixels. Current dimensions: %dx%d",
                        REQUIRED_WIDTH, REQUIRED_HEIGHT, width, height)
                );
            }
        } catch (IOException e) {
            log.error("Error validating image dimensions", e);
            throw new IllegalArgumentException("Error validating image dimensions: " + e.getMessage());
        }
        log.info("File validation successful - correct size, type and dimensions");
    }


}

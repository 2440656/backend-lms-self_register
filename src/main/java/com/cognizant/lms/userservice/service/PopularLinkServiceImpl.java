package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.PopularLinkDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.PopularLinkDto;
import com.cognizant.lms.userservice.dto.PopularLinkRequestDto;
import com.cognizant.lms.userservice.dto.PopularLinksResponse;
import lombok.extern.slf4j.Slf4j;
import com.cognizant.lms.userservice.exception.PopularLinkLimitException;
import com.cognizant.lms.userservice.exception.PopularLinkNotFoundException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;

import java.time.Instant;
import java.util.*;
import com.cognizant.lms.userservice.constants.Constants;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@Slf4j
public class PopularLinkServiceImpl implements PopularLinkService {
    private static final long MAX_ICON_SIZE_BYTES = 1 * 1024 * 1024;

    private final PopularLinkDao popularLinkDao;
    private final S3Client s3ThumbnailClient;

    @Value("${AWS_ICONS_S3_BUCKET_NAME}")
    private String bucketName;

    @Autowired
    public PopularLinkServiceImpl(PopularLinkDao popularLinkDao, S3Client s3ThumbnailClient) {
        this.popularLinkDao = popularLinkDao;
        this.s3ThumbnailClient = s3ThumbnailClient;
    }

    private PopularLinkDto toDto(Tenant tenant) {
        if (tenant == null) return null;
        return new PopularLinkDto(
                tenant.getLinkId(),
                tenant.getTitle(),
                tenant.getUrl(),
                tenant.getDescription(),
                tenant.getIndex(),
                tenant.getIconKey(),
                tenant.getIconFileName()
        );
    }

    private Tenant toEntity(PopularLinkRequestDto dto) {
        Tenant entity = new Tenant();
        entity.setLinkId(dto.getLinkId());
        entity.setTitle(dto.getTitle());
        entity.setUrl(dto.getUrl());
        entity.setDescription(dto.getDescription());
        // Set pk as tenantCode and sk as POPULARLINK#<linkId>
        String tenantCode = TenantUtil.getTenantCode();
        entity.setPk(tenantCode);
        String linkId = dto.getLinkId() != null ? dto.getLinkId() : "";
        entity.setSk("POPULARLINK#" + linkId);
        return entity;
    }

    @Override
    public PopularLinkDto savePopularLink(PopularLinkRequestDto linkRequest, MultipartFile file ) {
        Tenant tenant = toEntity(linkRequest);
        log.info("Inside save PopularLinks method serviceimpl");
        if (tenant.getLinkId() == null || tenant.getLinkId().isEmpty()) {
            String newLinkId = UUID.randomUUID().toString();
            tenant.setLinkId(newLinkId);
            tenant.setSk("POPULARLINK#" + newLinkId);
        }
        String username = UserContext.getCreatedBy();
        tenant.setCreatedBy(username);
        tenant.setCreatedOn(Instant.now().toString());

        List<Tenant> tenantLinks = popularLinkDao.getPopularLinksByTenant(tenant.getPk());
        log.info("Fetched existing popular links for tenant"+tenantLinks);
        if (tenantLinks.size() >= Constants.MAX_POPULAR_LINKS) {
            log.info("Popular links limit reached for tenant");
            throw new PopularLinkLimitException("Popular links reached maximum limit for this tenant");
        }
        log.info("Processing icon file upload if present: {}", file != null ? file.getOriginalFilename() : "no file");
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_ICON_SIZE_BYTES) {
                log.error("Icon file size exceeds max allowed for tenant ");
                throw new IllegalArgumentException("Icon file size must be less than or equal to 1 MB");
            }
            try {
                byte[] bytes = file.getBytes();
                String key = "popular-links/icons/" + tenant.getPk() + "/" + tenant.getLinkId() + "-" + UUID.randomUUID() + "-" + Optional.ofNullable(file.getOriginalFilename()).orElse("icon");
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))// optional: change according to requirements
                        .build();

                s3ThumbnailClient.putObject(putReq, RequestBody.fromBytes(bytes));
                log.info("Uploaded icon to S3");
                tenant.setIconKey(key);
                tenant.setIconFileName(file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Failed to process file upload ");
                throw new RuntimeException("Failed to process file upload", e);
            }
        }
        // Find the lowest available index in [1, 20]
        Set<Integer> usedIndices = tenantLinks.stream()
                .map(Tenant::getIndex)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int newIndex = IntStream.rangeClosed(1, Constants.MAX_POPULAR_LINKS)
                .filter(i -> !usedIndices.contains(i))
                .findFirst()
                .orElseThrow(() -> new PopularLinkLimitException("Popular links reached maximum limit for this tenant"));
        // Assign index and save
        tenant.setIndex(newIndex);
        tenant.setAction("add");
        popularLinkDao.savePopularLink(tenant);
        log.info("Popular link saved for tenant with linkId");
        PopularLinkDto dto = toDto(tenant);
        return dto;
    }

    @Override
    public PopularLinksResponse getAllPopularLinks() {
        String tenantCode = TenantUtil.getTenantCode();
        log.info("Inside get all popular links serviceimpl for tenant: {}", tenantCode);
        List<Tenant> linkList = popularLinkDao.getPopularLinksByTenant(tenantCode);
        log.info("Fetched {} popular links for tenant: {}", linkList.size(), linkList);
        List<PopularLinkDto> dtoList = linkList.stream()
                .map(link -> toDto(link))
                .toList();
        PopularLinksResponse response = new PopularLinksResponse();
        response.setPopularLinkList(dtoList);
        response.setCount(dtoList.size());
        return response;
    }

    @Override
    public PopularLinkDto updatePopularLink(PopularLinkRequestDto updateLinkRequest, MultipartFile file,
        boolean keepExistingFile, boolean removeExistingFile) {
        log.info("Inside update popularlink serviceimpl with linkId: {}", updateLinkRequest.getLinkId());
        if (updateLinkRequest.getLinkId() == null || updateLinkRequest.getLinkId().isEmpty()) {
            throw new IllegalArgumentException("linkId must be provided for update");
        }

        Tenant tenant = toEntity(updateLinkRequest);
        String tenantCode = TenantUtil.getTenantCode();
        String pk = tenantCode;
        String sk = "POPULARLINK#" + updateLinkRequest.getLinkId();
        Tenant existing = popularLinkDao.getPopularLinkByPk(pk, sk);

        if (existing == null) {
            throw new PopularLinkNotFoundException("PopularLink not found for update");
        }

        if (updateLinkRequest.getTitle() != null) existing.setTitle(updateLinkRequest.getTitle());
        if (updateLinkRequest.getUrl() != null) existing.setUrl(updateLinkRequest.getUrl());
        if (updateLinkRequest.getDescription() != null) existing.setDescription(updateLinkRequest.getDescription());
        log.info("file status",file);
        if (file != null && !file.isEmpty()) {
            log.info("Uploading new file to replace existing icon for linkId: {}", updateLinkRequest.getLinkId());
            if (existing.getIconKey() != null && !existing.getIconKey().isEmpty()) {
                log.info("Deleting existing icon from S3 if user want to replace the image: {}", existing.getIconKey());
                try {
                    DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(existing.getIconKey())
                            .build();
                    s3ThumbnailClient.deleteObject(delReq);
                    log.info("Deleted existing icon from S3: {}", existing.getIconKey());
                } catch (S3Exception e) {
                    log.error("Failed to delete existing S3 object {}: {}", existing.getIconKey(), e.getMessage(), e);
                }
            }

            try {
                if (file.getSize() > MAX_ICON_SIZE_BYTES) {
                    throw new IllegalArgumentException("Icon file size must be less than or equal to 1 MB");
                }

                byte[] bytes = file.getBytes();
                String key = "popular-links/icons/" + tenant.getPk() + updateLinkRequest.getLinkId() + "-" + UUID.randomUUID() + "-" +
                        Optional.ofNullable(file.getOriginalFilename()).orElse("icon");
                log.info("Uploading new icon to S3 with key: {}", key);
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))
                        .build();

                s3ThumbnailClient.putObject(putReq, RequestBody.fromBytes(bytes));
                existing.setIconFileName(file.getOriginalFilename());
                existing.setIconKey(key);
                log.info("Successfully uploaded new icon to S3: {}", key);
            } catch (Exception e) {
                log.error("Failed to upload new icon to S3 for linkId: {}", updateLinkRequest.getLinkId(), e);
                throw new RuntimeException("Failed to upload new icon to S3", e);
            }

        } else if (keepExistingFile) {
            log.info("Keeping existing icon for linkId: {}", updateLinkRequest.getLinkId());

        } else if (removeExistingFile) {
            log.info("Removing existing icon for linkId: {}", updateLinkRequest.getLinkId());

            if (existing.getIconKey() != null && !existing.getIconKey().isEmpty()) {
                try {
                    DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(existing.getIconKey())
                            .build();
                    s3ThumbnailClient.deleteObject(delReq);
                    log.info("Successfully deleted icon from S3: {}", existing.getIconKey());
                } catch (S3Exception e) {
                    log.error("Failed to delete S3 object {}: {}", existing.getIconKey(), e.getMessage(), e);
                    throw new RuntimeException("Failed to delete existing icon from S3", e);
                }
            }
            log.info("Clearing icon metadata for linkId: {}", updateLinkRequest.getLinkId());
            existing.setIconKey(null);
            existing.setIconFileName(null);

        } else {
            log.info("No file operation specified for linkId: {}, keeping existing icon", updateLinkRequest.getLinkId());
        }
        log.info("Updating metadata for popular link with linkId: {}", updateLinkRequest.getLinkId());
        existing.setAction("edit");
        existing.setModifiedOn(Instant.now().toString());
        existing.setModifiedBy(UserContext.getCreatedBy());
        log.info("Saving updated popular link to database for linkId: {}", updateLinkRequest.getLinkId());
        popularLinkDao.updatePopularLink(existing);

        log.info("Successfully updated popular link with linkId: {}", updateLinkRequest.getLinkId());
        return toDto(existing);
    }

    @Override
    public String deletePopularLink(String linkId) {
        if (linkId == null || linkId.isEmpty()) {
            throw new IllegalArgumentException("linkId must be provided for delete");
        }
        String tenantCode = TenantUtil.getTenantCode();
        String pk = tenantCode;
        String sk = "POPULARLINK#" + linkId;
        Tenant existing = popularLinkDao.getPopularLinkByPk(pk, sk);
        if (existing == null) {
            throw new PopularLinkNotFoundException("PopularLink not found for delete");
        }
        if (existing.getIconKey() != null && !existing.getIconKey().isEmpty()) {
            try {
                DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(existing.getIconKey())
                        .build();
                s3ThumbnailClient.deleteObject(delReq);
                log.info("Deleted icon from S3: {}", existing.getIconKey());
            } catch (S3Exception e) {
                log.error("Failed to delete S3 object {}: {}", existing.getIconKey(), e.getMessage(), e);
                throw new RuntimeException("Failed to delete icon from S3", e);
            }
        }
        int deletedIndex = existing.getIndex();
        // Delete the link
        popularLinkDao.deletePopularLink(pk, sk);
        // Fetch all links for this tenant
        List<Tenant> tenantLinks = popularLinkDao.getPopularLinksByTenant(tenantCode);
        // Find links with index > deletedIndex and decrement their index
        List<Tenant> linksToUpdate = new ArrayList<>();
        for (Tenant link : tenantLinks) {
            if (link.getIndex() != null && link.getIndex() > deletedIndex) {
                link.setIndex(link.getIndex() - 1);
                linksToUpdate.add(link);
            }
        }
        if (!linksToUpdate.isEmpty()) {
            popularLinkDao.updatePopularLinksTransactional(linksToUpdate);
        }
        return linkId;
    }

    @Override
    public PopularLinksResponse reorderPopularLink(PopularLinkRequestDto reorderRequest) {
        if (reorderRequest == null || reorderRequest.getLinkId() == null || reorderRequest.getIndex() == null) {
            log.error("Invalid reorder request: {}", reorderRequest);
            throw new IllegalArgumentException("linkId and index must be provided for reorder");
        }
        String tenantCode = TenantUtil.getTenantCode();
        List<Tenant> tenantLinks = popularLinkDao.getPopularLinksByTenant(tenantCode);
        // Find the link to move (stream for effectively final variable)
        String sk = "POPULARLINK#" + reorderRequest.getLinkId();
        Tenant movedLink = tenantLinks.stream()
                .filter(link -> sk.equals(link.getSk()))
                .findFirst()
                .orElseThrow(() -> new PopularLinkNotFoundException("PopularLink not found for reorder"));
        int oldIndex = movedLink.getIndex();
        int newIndex = reorderRequest.getIndex();
        int totalLinks = tenantLinks.size();
        if (newIndex < 1 || newIndex > totalLinks) {
            log.error("New index {} out of bounds for tenant {}", newIndex, tenantCode);
            throw new IllegalArgumentException("index out of bounds");
        }
        if (oldIndex == newIndex) {
            log.info("Old index and new index are the same. No reorder needed.");
            return null;
        }
        List<Tenant> linksToUpdate = new ArrayList<>();
        // Move up
        if (newIndex < oldIndex) {
            for (Tenant link : tenantLinks) {
                int idx = link.getIndex();
                if (idx >= newIndex && idx < oldIndex) {
                    link.setIndex(idx + 1);
                    linksToUpdate.add(link);
                }
            }
        } else { // Move down
            for (Tenant link : tenantLinks) {
                int idx = link.getIndex();
                if (idx > oldIndex && idx <= newIndex) {
                    link.setIndex(idx - 1);
                    linksToUpdate.add(link);
                }
            }
        }
        movedLink.setIndex(newIndex);
        linksToUpdate.add(movedLink);
        // Transactional update for atomicity
        popularLinkDao.updatePopularLinksTransactional(linksToUpdate);
        // Fetch and return the latest reordered list for this tenant
        List<Tenant> updatedLinks = popularLinkDao.getPopularLinksByTenant(tenantCode);
        List<PopularLinkDto> dtoList = updatedLinks.stream().map(this::toDto).toList();
        PopularLinksResponse response = new PopularLinksResponse();
        response.setPopularLinkList(dtoList);
        response.setCount(dtoList.size());
        log.info("Reorder complete for tenant {}. Total links: {}", tenantCode, dtoList.size());
        return response;
    }
}

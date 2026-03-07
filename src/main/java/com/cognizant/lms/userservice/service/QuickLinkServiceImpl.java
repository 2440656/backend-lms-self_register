package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.QuickLinkDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.QuickLinkDto;
import com.cognizant.lms.userservice.dto.QuickLinkRequestDto;
import com.cognizant.lms.userservice.dto.QuickLinksResponse;
import com.cognizant.lms.userservice.exception.QuickLinkLimitException;
import com.cognizant.lms.userservice.exception.QuickLinkNotFoundException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import com.cognizant.lms.userservice.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class QuickLinkServiceImpl implements QuickLinkService {

    private static final String ACTION_ADD = "add";
    private static final String ACTION_EDIT = "edit";

    private final QuickLinkDao quickLinkDao;

    @Autowired
    public QuickLinkServiceImpl(QuickLinkDao quickLinkDao) {
        this.quickLinkDao = quickLinkDao;
    }

    @Override
    public QuickLinkDto saveQuickLink(QuickLinkRequestDto linkRequest) {
        log.info("Saving new quick link with title: {}", linkRequest.getTitle());

        Tenant tenant = toEntity(linkRequest);
        String tenantCode = TenantUtil.getTenantCode();

        generateLinkIdIfNotPresent(tenant);
        setAuditFieldsForCreation(tenant);

        List<Tenant> existingLinks = quickLinkDao.getQuickLinksByTenant(tenantCode);
        log.debug("Found {} existing quick links for tenant: {}", existingLinks.size(), tenantCode);

        validateQuickLinkLimit(existingLinks);

        int newIndex = findNextAvailableIndex(existingLinks);
        tenant.setIndex(newIndex);
        tenant.setAction(ACTION_ADD);

        quickLinkDao.saveQuickLink(tenant);
        log.info("Quick link saved successfully with linkId: {} at index: {}", tenant.getLinkId(), newIndex);

        return toDto(tenant);
    }

    @Override
    public QuickLinksResponse getAllQuickLinks() {
        String tenantCode = TenantUtil.getTenantCode();
        log.info("Fetching all quick links for tenant: {}", tenantCode);

        List<Tenant> linkList = quickLinkDao.getQuickLinksByTenant(tenantCode);
        log.debug("Retrieved {} quick links", linkList.size());

        List<QuickLinkDto> sortedDtoList = linkList.stream()
                .sorted(Comparator.comparing(Tenant::getIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDto)
                .collect(Collectors.toList());

        return new QuickLinksResponse(sortedDtoList, sortedDtoList.size());
    }

    @Override
    public QuickLinkDto updateQuickLink(QuickLinkRequestDto updateLinkRequest) {
        log.info("Updating quick link with linkId: {}", updateLinkRequest.getLinkId());

        validateLinkIdPresent(updateLinkRequest.getLinkId());

        String tenantCode = TenantUtil.getTenantCode();
        String sk = buildSortKey(updateLinkRequest.getLinkId());

        Tenant existing = quickLinkDao.getQuickLinkByPk(tenantCode, sk);
        validateQuickLinkExists(existing, updateLinkRequest.getLinkId());

        updateQuickLinkFields(existing, updateLinkRequest);
        setAuditFieldsForUpdate(existing);

        quickLinkDao.updateQuickLink(existing);
        log.info("Quick link updated successfully: {}", existing.getLinkId());

        return toDto(existing);
    }

    @Override
    public String deleteQuickLink(String linkId) {
        log.info("Deleting quick link with linkId: {}", linkId);

        validateLinkIdPresent(linkId);

        String tenantCode = TenantUtil.getTenantCode();
        String sk = buildSortKey(linkId);

        Tenant existing = quickLinkDao.getQuickLinkByPk(tenantCode, sk);
        validateQuickLinkExists(existing, linkId);

        int deletedIndex = existing.getIndex();
        quickLinkDao.deleteQuickLink(tenantCode, sk);
        log.info("Quick link deleted: {}", linkId);

        reindexAfterDeletion(tenantCode, deletedIndex);

        return linkId;
    }

    @Override
    public QuickLinksResponse reorderQuickLink(QuickLinkRequestDto reorderRequest) {
        log.info("Reordering quick link {} to index {}", reorderRequest.getLinkId(), reorderRequest.getIndex());

        validateReorderRequest(reorderRequest);

        String tenantCode = TenantUtil.getTenantCode();
        List<Tenant> tenantLinks = quickLinkDao.getQuickLinksByTenant(tenantCode);

        String sk = buildSortKey(reorderRequest.getLinkId());
        Tenant movedLink = findQuickLinkBySk(tenantLinks, sk);

        int oldIndex = movedLink.getIndex();
        int newIndex = reorderRequest.getIndex();

        validateIndexInBounds(newIndex, tenantLinks.size());

        if (oldIndex == newIndex) {
            log.info("Link already at target index. No reordering needed.");
            return getAllQuickLinks();
        }

        List<Tenant> linksToUpdate = calculateReorderUpdates(tenantLinks, oldIndex, newIndex, movedLink);

        if (!linksToUpdate.isEmpty()) {
            quickLinkDao.updateQuickLinksTransactional(linksToUpdate);
            log.info("Successfully reordered {} quick links", linksToUpdate.size());
        }

        return getAllQuickLinks();
    }

    // ==================== Private Helper Methods ====================

    private QuickLinkDto toDto(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        return new QuickLinkDto(
                tenant.getLinkId(),
                tenant.getTitle(),
                tenant.getUrl(),
                tenant.getDescription(),
                tenant.getIndex()
        );
    }

    private Tenant toEntity(QuickLinkRequestDto dto) {
        Tenant entity = new Tenant();
        entity.setLinkId(dto.getLinkId());
        entity.setTitle(dto.getTitle());
        entity.setUrl(dto.getUrl());
        entity.setDescription(dto.getDescription());

        String tenantCode = TenantUtil.getTenantCode();
        entity.setPk(tenantCode);

        String linkId = Optional.ofNullable(dto.getLinkId()).orElse("");
        entity.setSk(buildSortKey(linkId));

        return entity;
    }

    private String buildSortKey(String linkId) {
        return Constants.QUICKLINK_PREFIX + linkId;
    }

    private void generateLinkIdIfNotPresent(Tenant tenant) {
        if (tenant.getLinkId() == null || tenant.getLinkId().isEmpty()) {
            String newLinkId = UUID.randomUUID().toString();
            tenant.setLinkId(newLinkId);
            tenant.setSk(buildSortKey(newLinkId));
            log.debug("Generated new linkId: {}", newLinkId);
        }
    }

    private void setAuditFieldsForCreation(Tenant tenant) {
        String username = UserContext.getCreatedBy();
        tenant.setCreatedBy(username);
        tenant.setCreatedOn(Instant.now().toString());
    }

    private void setAuditFieldsForUpdate(Tenant tenant) {
        tenant.setAction(ACTION_EDIT);
        tenant.setModifiedOn(Instant.now().toString());
        tenant.setModifiedBy(UserContext.getCreatedBy());
    }

    private void validateQuickLinkLimit(List<Tenant> existingLinks) {
        if (existingLinks.size() >= Constants.MAX_QUICK_LINKS) {
            log.warn("Quick links limit reached. Current count: {}, Max: {}",
                    existingLinks.size(), Constants.MAX_QUICK_LINKS);
            throw new QuickLinkLimitException(
                    "Quick links reached maximum limit of " + Constants.MAX_QUICK_LINKS);
        }
    }

    private int findNextAvailableIndex(List<Tenant> existingLinks) {
        Set<Integer> usedIndices = existingLinks.stream()
                .map(Tenant::getIndex)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return IntStream.rangeClosed(1, Constants.MAX_QUICK_LINKS)
                .filter(i -> !usedIndices.contains(i))
                .findFirst()
                .orElseThrow(() -> new QuickLinkLimitException(
                        "No available index found. Maximum limit reached."));
    }

    private void validateLinkIdPresent(String linkId) {
        if (linkId == null || linkId.isEmpty()) {
            throw new IllegalArgumentException("linkId must be provided");
        }
    }

    private void validateQuickLinkExists(Tenant tenant, String linkId) {
        if (tenant == null) {
            log.error("QuickLink not found with linkId: {}", linkId);
            throw new QuickLinkNotFoundException("QuickLink not found with linkId: " + linkId);
        }
    }

    private void updateQuickLinkFields(Tenant existing, QuickLinkRequestDto updateRequest) {
        if (updateRequest.getTitle() != null) {
            existing.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getUrl() != null) {
            existing.setUrl(updateRequest.getUrl());
        }
        if (updateRequest.getDescription() != null) {
            existing.setDescription(updateRequest.getDescription());
        }
    }

    private void reindexAfterDeletion(String tenantCode, int deletedIndex) {
        List<Tenant> remainingLinks = quickLinkDao.getQuickLinksByTenant(tenantCode);

        List<Tenant> linksToUpdate = remainingLinks.stream()
                .filter(link -> link.getIndex() != null && link.getIndex() > deletedIndex)
                .peek(link -> link.setIndex(link.getIndex() - 1))
                .collect(Collectors.toList());

        if (!linksToUpdate.isEmpty()) {
            log.info("Reindexing {} quick links after deletion", linksToUpdate.size());
            quickLinkDao.updateQuickLinksTransactional(linksToUpdate);
        }
    }

    private void validateReorderRequest(QuickLinkRequestDto reorderRequest) {
        if (reorderRequest == null ||
                reorderRequest.getLinkId() == null ||
                reorderRequest.getIndex() == null) {
            throw new IllegalArgumentException("linkId and index must be provided for reorder");
        }
    }

    private Tenant findQuickLinkBySk(List<Tenant> links, String sk) {
        return links.stream()
                .filter(link -> sk.equals(link.getSk()))
                .findFirst()
                .orElseThrow(() -> new QuickLinkNotFoundException("QuickLink not found for reorder"));
    }

    private void validateIndexInBounds(int newIndex, int totalLinks) {
        if (newIndex < 1 || newIndex > totalLinks) {
            throw new IllegalArgumentException(
                    String.format("Index %d out of bounds. Must be between 1 and %d",
                            newIndex, totalLinks));
        }
    }

    private List<Tenant> calculateReorderUpdates(List<Tenant> allLinks, int oldIndex,
                                                 int newIndex, Tenant movedLink) {
        List<Tenant> linksToUpdate = new ArrayList<>();

        if (newIndex < oldIndex) {
            // Moving up: shift affected links down
            allLinks.stream()
                    .filter(link -> {
                        int idx = link.getIndex();
                        return idx >= newIndex && idx < oldIndex;
                    })
                    .forEach(link -> {
                        link.setIndex(link.getIndex() + 1);
                        linksToUpdate.add(link);
                    });
        } else {
            // Moving down: shift affected links up
            allLinks.stream()
                    .filter(link -> {
                        int idx = link.getIndex();
                        return idx > oldIndex && idx <= newIndex;
                    })
                    .forEach(link -> {
                        link.setIndex(link.getIndex() - 1);
                        linksToUpdate.add(link);
                    });
        }

        movedLink.setIndex(newIndex);
        linksToUpdate.add(movedLink);

        return linksToUpdate;
    }
}
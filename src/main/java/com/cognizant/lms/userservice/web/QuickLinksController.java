package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.QuickLinkDto;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.QuickLinkRequestDto;
import com.cognizant.lms.userservice.dto.QuickLinksResponse;
import com.cognizant.lms.userservice.exception.QuickLinkLimitException;
import com.cognizant.lms.userservice.service.QuickLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/quickLinks")
public class QuickLinksController {
    private final QuickLinkService quickLinkService;

    @Autowired
    public QuickLinksController(QuickLinkService quickLinkService) {
        this.quickLinkService = quickLinkService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> saveQuickLink(@RequestBody QuickLinkRequestDto linkRequest) {
        log.info("Inside saveQuickLink method of QuickLinksController");
        HttpResponse response = new HttpResponse();

        if (linkRequest.getTitle() == null || linkRequest.getTitle().trim().isEmpty() ||
                linkRequest.getUrl() == null || linkRequest.getUrl().trim().isEmpty() ||
                linkRequest.getDescription() == null || linkRequest.getDescription().trim().isEmpty()) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Title, URL and description must not be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            QuickLinkDto dto = quickLinkService.saveQuickLink(linkRequest);
            response.setData(dto);
            response.setStatus(HttpStatus.CREATED.value());
            response.setError(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (QuickLinkLimitException e) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error saving quick link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to save quick link: " + (e.getMessage() != null ? e.getMessage() : "internal error"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin','learner','content-author','mentor')")
    public ResponseEntity<HttpResponse> getAllQuickLinks() {
        log.info("Inside getAllQuickLinks method of QuickLinksController");
        HttpResponse response = new HttpResponse();

        try {
            QuickLinksResponse quickLinksResponse = quickLinkService.getAllQuickLinks();
            response.setData(quickLinksResponse);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching quick links: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to fetch quick links");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> updateQuickLink(@RequestBody QuickLinkRequestDto updateLinkRequest) {
        HttpResponse response = new HttpResponse();

        try {
            if (updateLinkRequest.getLinkId() == null || updateLinkRequest.getLinkId().isEmpty() ||
                    updateLinkRequest.getTitle() == null || updateLinkRequest.getTitle().trim().isEmpty() ||
                    updateLinkRequest.getUrl() == null || updateLinkRequest.getUrl().trim().isEmpty() ||
                    updateLinkRequest.getDescription() == null || updateLinkRequest.getDescription().trim().isEmpty()) {
                response.setData(null);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("LinkId, title, URL and description must not be empty");
                return ResponseEntity.badRequest().body(response);
            }

            response.setData(quickLinkService.updateQuickLink(updateLinkRequest));
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating quick link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to update quick link");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> deleteQuickLink(@RequestParam("linkId") String linkId) {
        HttpResponse response = new HttpResponse();

        try {
            if (linkId == null || linkId.isEmpty()) {
                response.setData(null);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("linkId must be provided for delete");
                return ResponseEntity.badRequest().body(response);
            }

            response.setData(quickLinkService.deleteQuickLink(linkId));
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting quick link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to delete quick link");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> reorderQuickLink(@RequestBody QuickLinkRequestDto reorderRequest) {
        HttpResponse response = new HttpResponse();

        try {
            if (reorderRequest.getLinkId() == null || reorderRequest.getIndex() == null) {
                response.setData(null);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("linkId and index must be provided for reorder");
                return ResponseEntity.badRequest().body(response);
            }

            QuickLinksResponse reorderedLinks = quickLinkService.reorderQuickLink(reorderRequest);
            response.setData(reorderedLinks);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reordering quick link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to reorder quick link");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

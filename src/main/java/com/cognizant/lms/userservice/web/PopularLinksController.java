package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.PopularLinkDto;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.PopularLinkRequestDto;
import com.cognizant.lms.userservice.dto.PopularLinksResponse;
import com.cognizant.lms.userservice.exception.PopularLinkLimitException;
import com.cognizant.lms.userservice.service.PopularLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("api/v1/popularLinks")
public class PopularLinksController {
    private final PopularLinkService popularLinkService;

    @Autowired
    public PopularLinksController(PopularLinkService popularLinkService) {
        this.popularLinkService = popularLinkService;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<com.cognizant.lms.userservice.dto.HttpResponse> savePopularLink(
            @RequestPart("linkRequest") String linkRequestJson,
            @RequestPart(value = "file", required = false) MultipartFile file){
        log.info("Inside savePopularLink method of PopularLinksController");
        HttpResponse response = new HttpResponse();
        PopularLinkRequestDto linkRequest;
        try {
            linkRequest = objectMapper.readValue(linkRequestJson, PopularLinkRequestDto.class);
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Request body, title,URL and description must not be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (linkRequest.getTitle() == null || linkRequest.getTitle().trim().isEmpty() ||
                linkRequest.getUrl() == null || linkRequest.getUrl().trim().isEmpty() ||
                linkRequest.getDescription() == null || linkRequest.getDescription().trim().isEmpty()) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Request body, title and URL must not be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        try {
            PopularLinkDto dto = popularLinkService.savePopularLink(linkRequest, file);
            response.setData(dto);
            response.setStatus(HttpStatus.CREATED.value());
            response.setError(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PopularLinkLimitException e) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error saving popular link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to save popular link: " + (e.getMessage() != null ? e.getMessage() : "internal error"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin','learner','content-author','mentor')")
    public ResponseEntity<HttpResponse> getAllPopularLinks() {
        log.info("Inside getAllPopularLinks method of PopularLinksController");
        HttpResponse response = new HttpResponse();
        try {
            PopularLinksResponse popularLinksResponse = popularLinkService.getAllPopularLinks();
            response.setData(popularLinksResponse);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching popular links: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to fetch popular links");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> updatePopularLink(
            @RequestPart(value= "linkRequest") String linkRequestJson,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "keepExistingFile", required = false) String keepExistingFile,
            @RequestPart(value = "removeExistingFile", required = false) String removeExistingFile) {
        HttpResponse response = new HttpResponse();
        log.info("linkRequestJson for editPopularLink: {}", linkRequestJson);
        PopularLinkRequestDto updateLinkRequest;
        try {
            updateLinkRequest = objectMapper.readValue(linkRequestJson, PopularLinkRequestDto.class);
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Invalid linkRequest JSON");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            if (updateLinkRequest.getLinkId() == null || updateLinkRequest.getLinkId().isEmpty() ||
                    updateLinkRequest.getTitle() == null || updateLinkRequest.getTitle().trim().isEmpty() ||
                    updateLinkRequest.getUrl() == null || updateLinkRequest.getUrl().trim().isEmpty() ||
                    updateLinkRequest.getDescription() == null || updateLinkRequest.getDescription().trim().isEmpty()) {
                response.setData(null);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("Request body, title,URL and description must not be empty");
                return ResponseEntity.badRequest().body(response);
            }

            boolean shouldKeepExisting = "true".equals(keepExistingFile);
            boolean shouldRemoveExisting = "true".equals(removeExistingFile);

            log.info("Updating popular link - linkId: {}, hasNewFile: {}, keepExisting: {}, removeExisting: {}",
                    updateLinkRequest.getLinkId(), file != null, shouldKeepExisting, shouldRemoveExisting);

            response.setData(popularLinkService.updatePopularLink(updateLinkRequest, file, shouldKeepExisting, shouldRemoveExisting));
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating popular link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to update popular link");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> deletePopularLink(@RequestParam("linkId") String linkId) {
        HttpResponse response = new HttpResponse();
        log.info("Deleting popular link with linkId inside poplarlinks controller: {}", linkId);
        try {
            if (linkId == null || linkId.isEmpty()) {
                response.setData(null);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("linkId must be provided for delete");
                return ResponseEntity.badRequest().body(response);
            }
            response.setData(popularLinkService.deletePopularLink(linkId));
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting popular link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to delete popular link");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','catalog-admin')")
    public ResponseEntity<HttpResponse> reorderPopularLink(@RequestBody PopularLinkRequestDto reorderRequest) {
        HttpResponse response = new HttpResponse();
        try {
            if (reorderRequest.getLinkId() == null || reorderRequest.getIndex() == null) {
                response.setData(null);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setError("linkId and index must be provided for reorder");
                return ResponseEntity.badRequest().body(response);
            }
            PopularLinksResponse reorderedLinks = popularLinkService.reorderPopularLink(reorderRequest);
            response.setData(reorderedLinks);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reordering popular link: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setError("Failed to reorder popular link");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

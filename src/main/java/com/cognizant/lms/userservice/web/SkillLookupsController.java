package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.FileUploadResponse;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.OperationHistoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;
import com.cognizant.lms.userservice.dto.SkillMigrationEventDetailDto;
import com.cognizant.lms.userservice.service.OperationsHistoryService;
import com.cognizant.lms.userservice.service.SkillLookupsService;
import com.cognizant.lms.userservice.service.UserManagementEventPublisherService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.core.io.Resource;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/users/skillLookups")
public class SkillLookupsController {

    private final SkillLookupsService skillLookupsService;

    private final UserManagementEventPublisherService userManagementEventPublisherService;

    private final OperationsHistoryService operationsHistoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','catalog-admin')")
    public ResponseEntity<SkillLookupResponse> getSkillsLookupsByTypeNameOrCode(
        @RequestParam("type") String type,
        @RequestParam(value = "search", required = false) String search) {

        try {
            if (type == null || type.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new SkillLookupResponse(null, null, 400, "Skill lookups type cannot be empty")
                );
            }

            boolean hasSkillType = Arrays.stream(type.split(","))
                .map(String::trim)
                .anyMatch(t -> t.equalsIgnoreCase(Constants.SKILL));

            if (hasSkillType && (search == null || search.trim().isEmpty())) {
                return ResponseEntity.badRequest().body(
                    new SkillLookupResponse(null, null, 400, "Search cannot be empty for skill type")
                );
            }

            SkillLookupResponse skillLookupResponse =
                skillLookupsService.getSkillsLookupsByTypeNameOrCode(type, search);

            if (skillLookupResponse == null || skillLookupResponse.getSkills() == null
                || skillLookupResponse.getSkills().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                    new SkillLookupResponse(null, null, 204, "No skills found")
                );
            }

            skillLookupResponse.setStatus(200);
            skillLookupResponse.setError(null);
            return ResponseEntity.ok(skillLookupResponse);
        } catch (Exception e) {
            log.error("Error fetching skill lookups by type and search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new SkillLookupResponse(null, null, 500, e.getMessage())
            );
        }
    }

    @PostMapping("/backfill")
    @PreAuthorize("hasAnyRole('system-admin','super-admin', 'content-author')")
    public ResponseEntity<HttpResponse> backfillSkillLookups(
        @RequestParam(value = "lastEvaluatedKey", required = false) String lastEvaluatedKey) {

        try {
            SkillMigrationEventDetailDto eventDetail = new SkillMigrationEventDetailDto();
            eventDetail.setTenantCode(TenantUtil.getTenantCode());
            eventDetail.setEventType(Constants.MIGRATION_SKILL);
            eventDetail.setLastEvaluatedKey(lastEvaluatedKey);

            userManagementEventPublisherService.triggerSkillMigrationEvent(eventDetail);
            log.info("Skill lookups backfill triggered successfully with lastEvaluatedKey: {}", lastEvaluatedKey);
            return ResponseEntity.ok(new HttpResponse(null, HttpStatus.OK.value(), "Skill lookups backfill triggered successfully"));
        } catch (Exception e) {
            log.error("Backfill trigger failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new HttpResponse(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to trigger skill lookups backfill: " + e.getMessage())
            );
        }
    }



    @GetMapping("/skillCategory")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','content-author','catalog-admin','learner')")
    public ResponseEntity<HttpResponse> getSkillCategoryBySkillName(
        @RequestParam(value = "skillName") String skillName) {
        HttpResponse response = new HttpResponse();
        String decodedSkillName = URLDecoder.decode(skillName, StandardCharsets.UTF_8);
        if (decodedSkillName == null || decodedSkillName.isEmpty()) {
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setError("Skill Id cannot be empty");
            log.error("Skill Id cannot be empty");
            return ResponseEntity.badRequest().body(response);
        } else {
            response.setData(skillLookupsService.getSkillCategory(decodedSkillName));
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            log.info("Fetched skill category by skill id {}", response.getData());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('system-admin','super-admin')")
    public ResponseEntity<HttpResponse> uploadSkillsCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "action", defaultValue = Constants.ACTION_UPLOAD_SKILLS) String action) {
        HttpResponse response = new HttpResponse();
        String fileName = file != null ? file.getOriginalFilename() : "unknown";
        FileUploadResponse fileUploadResponse = new FileUploadResponse();
        try {
            log.info("Uploading skills CSV: file={}, action={}", fileName, action);
            fileUploadResponse = skillLookupsService.uploadSkills(file, action);
            response.setStatus(HttpStatus.OK.value());
            response.setData(fileUploadResponse);
            response.setError(null);
            log.info("Skills CSV uploaded successfully: file={}, action={}", fileName, action);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for skills CSV upload: file={}, action={}, error={}", fileName, action, e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setData(null);
            response.setError("Invalid input: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Skills CSV upload failed: file={}, action={}, error={}", fileName, action, e.getMessage(), e);
            String errorLogFileName = fileUploadResponse.getErrorLogFileName();
            if (errorLogFileName == null) {
                errorLogFileName = fileName + "_LOG.txt";
                fileUploadResponse.setErrorLogFileName(errorLogFileName);
            }
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setData(fileUploadResponse);
            response.setError("An error occurred while uploading skills CSV: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('system-admin','super-admin')")
    public ResponseEntity<Resource> downloadSkillsFile(@RequestParam("fileName") String filename,
                                                       @RequestParam("fileType") String fileType) {
        try {
            String sanitizedFilename = FilenameUtils.getName(filename);
            Path filePath = Paths.get(sanitizedFilename).normalize();
            if (filePath.startsWith("..") || filePath.isAbsolute()) {
                log.error("Invalid file name: {}", filename);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }
            Resource fileResource = skillLookupsService.getDownloadErrorLogFileForSkills(filePath.toString(), fileType);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, Constants.ATTACHMENT_FILENAME
                            + Constants.SLASH + filePath.getFileName().toString() + Constants.SLASH)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(fileResource);
        } catch (FileNotFoundException e) {
            log.error("File not found: {}", filename, e);
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(null);
        } catch (IOException e) {
            log.error("Error reading file: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        } catch (Exception e) {
            log.error("Unexpected error: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/importHistory")
    @PreAuthorize("hasAnyRole('system-admin','super-admin')")
    public ResponseEntity<OperationHistoryResponse> fetchSkillsImportHistory(
        @RequestParam(value = "sortKey", defaultValue = "createdOn") String sortKey,
        @RequestParam(value = "order", defaultValue = "desc") String order,
        @RequestParam(value = "process", required = false) String process,
        @RequestParam(value = "area", defaultValue = Constants.AREA_SKILL_MANAGEMENT) String area,
        @RequestParam(value = "lastEvaluatedKey", required = false) String lastEvaluatedKeyEncoded,
        @RequestParam(value = "perPage", defaultValue = "5", required = false) int perPage) {

        try{

            perPage = perPage < 0 || perPage > 100 ? 5 : perPage;
            OperationHistoryResponse response =
                operationsHistoryService.getLogFiles(sortKey, order, process, area, lastEvaluatedKeyEncoded, perPage);
            if (response.getStatus() == HttpStatus.BAD_REQUEST.value()) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        }catch (Exception e){
            log.error("Failed to fetch skills import history {} ", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}

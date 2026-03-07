package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.domain.SkillLookups;
import com.cognizant.lms.userservice.dto.SkillsCSVProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Component
public class SkillCSVProcessor {

    @Autowired
    private SkillCSVValidator skillCSVValidator;

    public SkillsCSVProcessResponse processFile(MultipartFile file) {
        List<SkillLookups> validSkills = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Map<String, SkillLookups> deduped = new LinkedHashMap<>();
        Set<String> seenCodes = new HashSet<>();
        int totalCount = 0;
        int failureCount = 0;
        int successCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(),
                StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(reader);

            // Validate headers
            boolean validationHeaders = skillCSVValidator.validateHeaders(parser.getHeaderMap(), Constants.VALID_SKILL_MASTER_HEADERS);
            if(validationHeaders) {
                int rowNum = Constants.INITIAL_ROW_NUM;
                for (CSVRecord record : parser) {
                    rowNum++;
                    totalCount++;
                    List<String> errorList = skillCSVValidator.validateSkillsFields(record, rowNum, seenCodes);
                    if (errorList.isEmpty()) {
                        SkillLookups skill = SkillLookups.builder()
                                .pk(TenantUtil.getTenantCode() + Constants.HASH + (!record.get("skillCategory").isEmpty() ? record.get("skillCategory").toUpperCase().substring(0,1) : "T"))
                                .sk(record.get("skillCode").trim())
                                .type("Skill")
                                .name(record.get("skillName").trim())
                                .active(record.get("status").equalsIgnoreCase(Constants.ACTIVE_STATUS) ? "y" : "n")
                                .gsiTypeSk(record.get("skillCode").trim().toLowerCase() + Constants.HASH + record.get("skillName").trim().toLowerCase())
                                .effectiveDate(LocalDateTime.now().format(DateTimeFormatter
                                        .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)))
                                .skillCode(record.get("skillCode").trim())
                                .skillName(record.get("skillName").trim())
                                .skillDescription(record.get("skillDescription").trim())
                                .skillType(record.get("skillType").trim())
                                .status(record.get("status").trim())
                                .skillCategory(record.get("skillCategory").trim())
                                .skillSubCategory(record.get("skillSubCategory").trim())
                                .build();
                        deduped.put(skill.getSkillCode(), skill);
                        successCount = successCount + 1;
                        log.info("successCount is " + successCount);
                    } else {
                        failureCount = failureCount + 1;
                        errorList.forEach(err -> errors.add(
                                String.format(
                                        "%s--%s--%s--%s--%s",
                                        LocalDateTime.now().format(DateTimeFormatter
                                                .ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                                        record.get("skillCode").trim(),
                                        record.get("skillName").trim(),
                                        record.get("skillType").trim(),
                                        err)));
                        log.info("failureCount is " + failureCount);
                    }
                }
            } else{
                log.info("failureCount is " + failureCount);
                errors.add(String.format(
                        "%s--%s",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                        "Invalid Header : Must be one of " + Constants.VALID_SKILL_MASTER_HEADERS));
            }
        } catch (Exception e) {
            log.error("Error while processing the file: " + e.getMessage());
            failureCount = failureCount + 1;
            errors.add(String.format(
                    "%s--%s",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                    "Error while processing the file: " + e.getMessage()));
        }
        validSkills.addAll(deduped.values());
        log.info("Processor Total count: " + totalCount);
        log.info("Processor Success count: " + successCount);
        log.info("Processor Failure count: " + failureCount);
        return new SkillsCSVProcessResponse(validSkills, errors, successCount, failureCount, totalCount);
    }
}

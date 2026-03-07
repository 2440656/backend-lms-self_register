package com.cognizant.lms.userservice.utils;

import org.apache.commons.csv.CSVRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SkillCSVValidatorTest {

    @InjectMocks
    private SkillCSVValidator skillCSVValidator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void validateHeaders_validHeaders_returnsTrue() {
        Map<String, Integer> headers = new LinkedHashMap<>();
        List<String> validHeaders = List.of(
                "skillCode", "skillName", "skillDescription", "skillType", "status", "skillCategory", "skillSubCategory"
        );
        validHeaders.forEach(h -> headers.put(h, 0));
        assertTrue(skillCSVValidator.validateHeaders(headers, validHeaders));
    }

    @Test
    void validateHeaders_missingHeaders_returnsFalse() {
        Map<String, Integer> headers = new LinkedHashMap<>();
        headers.put("skillCode", 0);
        headers.put("skillName", 1);
        List<String> validHeaders = List.of(
                "skillCode", "skillName", "skillDescription", "skillType", "status", "skillCategory", "skillSubCategory"
        );
        assertFalse(skillCSVValidator.validateHeaders(headers, validHeaders));
    }

    @Test
    void validateSkillsFields_allValid_returnsEmptyList() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                "skillCode", "SC001",
                "skillName", "Java",
                "skillDescription", "desc",
                "skillType", "TypeA",
                "status", "Active",
                "skillCategory", "Cat1",
                "skillSubCategory", "SubCat1"
        ));
        when(record.get(anyString())).thenAnswer(invocation -> record.toMap().get(invocation.getArgument(0)));
        List<String> errors = skillCSVValidator.validateSkillsFields(record, 2, new HashSet<>());
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateSkillsFields_missingMandatoryFields_returnsErrors() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                "skillCode", "",
                "skillName", "",
                "skillType", "",
                "status", "",
                "skillDescription", "",
                "skillCategory", "",
                "skillSubCategory", ""
        ));
        when(record.get(anyString())).thenAnswer(invocation -> record.toMap().get(invocation.getArgument(0)));
        List<String> errors = skillCSVValidator.validateSkillsFields(record, 2, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillCode is mandatory")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillName is mandatory")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillType is mandatory")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("status is mandatory")));
    }

    @Test
    void validateSkillsFields_fieldLengthAndPattern_returnsErrors() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.toMap()).thenReturn(Map.of(
                "skillCode", "A".repeat(91), // exceeds 90
                "skillName", "B".repeat(256), // exceeds 255
                "skillType", "Type1!", // invalid chars
                "status", "Unknown", // not Active/Inactive
                "skillDescription", "C".repeat(2001), // exceeds 2000
                "skillCategory", "D".repeat(256), // exceeds 255
                "skillSubCategory", "E".repeat(256) // exceeds 255
        ));
        when(record.get(anyString())).thenAnswer(invocation -> record.toMap().get(invocation.getArgument(0)));
        List<String> errors = skillCSVValidator.validateSkillsFields(record, 2, new HashSet<>());
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillCode exceeds 90 chars")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillName exceeds 255 chars")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillType must contain only letters and spaces")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("status must be either 'Active' or 'Inactive'")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillDescription exceeds 2000 chars")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillCategory exceeds 255 chars")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("skillSubCategory exceeds 255 chars")));
    }
}

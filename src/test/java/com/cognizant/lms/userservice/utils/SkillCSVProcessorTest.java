package com.cognizant.lms.userservice.utils;


import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.SkillsCSVProcessResponse;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SkillCSVProcessorTest {

    @Mock
    private SkillCSVValidator skillCSVValidator;

    @InjectMocks
    private SkillCSVProcessor skillCSVProcessor;

    @BeforeEach
    public void setUp() {
        // Initialize mocks and the SkillCSVProcessor instance
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processFile_validCSV_returnsSuccessResponse() throws Exception {
        String csv = "skillCode,skillName,skillDescription,skillType,status,skillCategory,skillSubCategory\n" +
                "SC001,Java,desc,TypeA,Active,Cat1,SubCat1\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(skillCSVValidator.validateHeaders(anyMap(), eq(Constants.VALID_SKILL_MASTER_HEADERS))).thenReturn(true);
        when(skillCSVValidator.validateSkillsFields(any(CSVRecord.class), anyInt(), anySet())).thenReturn(Collections.emptyList());

        SkillsCSVProcessResponse response = skillCSVProcessor.processFile(file);

        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(1, response.getValidSkills().size());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void processFile_invalidHeaders_returnsErrorResponse() throws Exception {
        String csv = "wrongHeader1,wrongHeader2\nSC001,Java\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(skillCSVValidator.validateHeaders(anyMap(), eq(Constants.VALID_SKILL_MASTER_HEADERS))).thenReturn(false);

        SkillsCSVProcessResponse response = skillCSVProcessor.processFile(file);

        assertEquals(0, response.getSuccessCount());
        assertEquals(0, response.getValidSkills().size());
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).contains("Invalid Header"));
    }

    @Test
    void processFile_invalidRow_returnsErrorResponse() throws Exception {
        String csv = "skillCode,skillName,skillDescription,skillType,status,skillCategory,skillSubCategory\n" +
                "SC001,Java,desc,TypeA,Active,Cat1,SubCat1\n" +
                "SC002,,desc,TypeB,Inactive,Cat2,SubCat2\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(skillCSVValidator.validateHeaders(anyMap(), eq(Constants.VALID_SKILL_MASTER_HEADERS))).thenReturn(true);
        when(skillCSVValidator.validateSkillsFields(any(CSVRecord.class), eq(2), anySet())).thenReturn(Collections.emptyList());
        when(skillCSVValidator.validateSkillsFields(any(CSVRecord.class), eq(3), anySet())).thenReturn(List.of("Skill name is required"));

        SkillsCSVProcessResponse response = skillCSVProcessor.processFile(file);

        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertEquals(1, response.getValidSkills().size());
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).contains("Skill name is required"));
    }

    @Test
    void processFile_exceptionThrown_returnsErrorResponse() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new RuntimeException("IO error"));

        SkillsCSVProcessResponse response = skillCSVProcessor.processFile(file);

        assertEquals(0, response.getSuccessCount());
        assertEquals(0, response.getValidSkills().size());
        assertEquals(1, response.getFailureCount());
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).contains("Error while processing the file"));
    }



}

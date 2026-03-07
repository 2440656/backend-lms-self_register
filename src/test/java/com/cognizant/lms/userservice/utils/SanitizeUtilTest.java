package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SanitizeUtilTest {

    private static final int MIN = 1;
    private static final int MAX = 100;

    @Test
    public void testSanitizeData() {
        SanitizeUtil sanitizeUtil = new SanitizeUtil();

        // Test with null input
        assertNull(sanitizeUtil.sanitizeData(null));

        // Test with empty input
        assertNull(sanitizeUtil.sanitizeData(""));

        // Test with valid input
        assertEquals("testdata", sanitizeUtil.sanitizeData("testdata"));

        // Test with input containing diacritical marks
        assertEquals("cafe", sanitizeUtil.sanitizeData("cafe"));

        // Test with input containing potential SSRF
        assertThrows(SecurityException.class, () -> sanitizeUtil.sanitizeData("http://example.com"));
        assertThrows(SecurityException.class, () -> sanitizeUtil.sanitizeData("https://example.com"));
        assertThrows(SecurityException.class, () -> sanitizeUtil.sanitizeData("file://example.com"));
    }

    @Test
    public void testSanitizeFileName() {
        // Test with null input
        assertNull(SanitizeUtil.sanitizeFileName(null));

        // Test with empty input
        assertEquals("", SanitizeUtil.sanitizeFileName(""));

        // Test with valid input
        assertEquals("filename", SanitizeUtil.sanitizeFileName("filename"));

        // Test with input containing diacritical marks
        assertEquals("cafe", SanitizeUtil.sanitizeFileName("cafe"));

        // Test with input containing potential SSRF
        assertThrows(SecurityException.class, () -> SanitizeUtil.sanitizeFileName("http://example.com"));
        assertThrows(SecurityException.class, () -> SanitizeUtil.sanitizeFileName("https://example.com"));
    }

    @Test
    public void testSanitizePath() {
        // Test with null input
        assertEquals("", SanitizeUtil.sanitizePath(null));

        // Test with empty input
        assertEquals("", SanitizeUtil.sanitizePath(""));

        // Test with valid input
        assertEquals("validpath", SanitizeUtil.sanitizePath("validpath"));

        // Test with input containing diacritical marks
        assertEquals("cafe", SanitizeUtil.sanitizePath("cafe"));

        // Test with input containing potential SSRF
        assertThrows(SecurityException.class, () -> SanitizeUtil.sanitizePath("http://example.com"));
        assertThrows(SecurityException.class, () -> SanitizeUtil.sanitizePath("https://example.com"));
    }

    @Test
    void testSanitizePerPage_BlankDefaultsToMin() {
        assertEquals(MIN, SanitizeUtil.sanitizePerPage("", MIN, MAX));
        assertEquals(MIN, SanitizeUtil.sanitizePerPage("   ", MIN, MAX));
    }

    @Test
    void testSanitizePerPage_NonNumericDefaultsToMin() {
        assertEquals(MIN, SanitizeUtil.sanitizePerPage("abc", MIN, MAX));
        assertEquals(MIN, SanitizeUtil.sanitizePerPage("12abc", MIN, MAX));
        assertEquals(MIN, SanitizeUtil.sanitizePerPage("1_2", MIN, MAX));
        assertEquals(MIN, SanitizeUtil.sanitizePerPage("9999999999", MIN, MAX)); // too long
    }

    @Test
    void testSanitizePerPage_WithinRangeReturnsParsed() {
        assertEquals(1, SanitizeUtil.sanitizePerPage("1", MIN, MAX));
        assertEquals(25, SanitizeUtil.sanitizePerPage("25", MIN, MAX));
        assertEquals(100, SanitizeUtil.sanitizePerPage("100", MIN, MAX));
    }

    @Test
    void testSanitizePerPage_AboveMaxClampsToMax() {
        assertEquals(MAX, SanitizeUtil.sanitizePerPage("101", MIN, MAX));
        assertEquals(MAX, SanitizeUtil.sanitizePerPage("999999999", MIN, MAX));
    }

    @Test
    void testSanitizePerPage_EqualToMinAndMax() {
        assertEquals(MIN, SanitizeUtil.sanitizePerPage(String.valueOf(MIN), MIN, MAX));
        assertEquals(MAX, SanitizeUtil.sanitizePerPage(String.valueOf(MAX), MIN, MAX));
    }

    @Test
    void testSanitizePerPage_HandlesCustomBounds() {
        assertEquals(5, SanitizeUtil.sanitizePerPage("5", 5, 10));
        assertEquals(10, SanitizeUtil.sanitizePerPage("15", 5, 10)); // clamp
        assertEquals(5, SanitizeUtil.sanitizePerPage("0", 5, 10));   // clamp
    }

}
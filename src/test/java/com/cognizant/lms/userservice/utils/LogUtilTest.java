package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogUtilTest {
    @Test
    void getLogInfo_validInput_returnsFormattedString() {
        String processName = "Process1";
        String status = "Success";
        String expected = "Process1 : Success : ";

        String result = LogUtil.getLogInfo(processName, status);

        assertEquals(expected, result);
    }

    @Test
    void getLogError_validInput_returnsFormattedString() {
        String processName = "Process1";
        String errorCode = "404";
        String status = "Error";
        String expected = "Process1 : 404 : Error";

        String result = LogUtil.getLogError(processName, errorCode, status);

        assertEquals(expected, result);
    }

    @Test
    void getLogInfo_nullInput_returnsFormattedStringWithNull() {
        String processName = null;
        String status = null;
        String expected = "null : null : ";

        String result = LogUtil.getLogInfo(processName, status);

        assertEquals(expected, result);
    }

    @Test
    void getLogError_nullInput_returnsFormattedStringWithNull() {
        String processName = null;
        String errorCode = null;
        String status = null;
        String expected = "null : null : null";

        String result = LogUtil.getLogError(processName, errorCode, status);

        assertEquals(expected, result);
    }
}

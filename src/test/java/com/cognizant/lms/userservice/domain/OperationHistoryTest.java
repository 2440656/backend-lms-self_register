package com.cognizant.lms.userservice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class OperationsHistoryTest {

    @Test
    void testNoArgsConstructor() {
        OperationsHistory operationsHistory = new OperationsHistory();
        assertNull(operationsHistory.getPk());
        assertNull(operationsHistory.getSk());
        assertNull(operationsHistory.getFileName());
        assertNull(operationsHistory.getUploadedBy());
        assertNull(operationsHistory.getCreatedOn());
        assertNull(operationsHistory.getEmail());
        assertNull(operationsHistory.getTenantCode());
        assertNull(operationsHistory.getOperation());
        assertNull(operationsHistory.getArea());
        assertNull(operationsHistory.getUploadStatus());

    }

    @Test
    void testAllArgsConstructor() {
        OperationsHistory operationsHistory = new OperationsHistory(
                "pk1", "sk1", "fileName1", "uploadedBy1", "createdOn1",
                "email1", "tenantCode1", "operation1", "area1", "uploadStatus1", "", "", ""
        );

        assertEquals("pk1", operationsHistory.getPk());
        assertEquals("sk1", operationsHistory.getSk());
        assertEquals("fileName1", operationsHistory.getFileName());
        assertEquals("uploadedBy1", operationsHistory.getUploadedBy());
        assertEquals("createdOn1", operationsHistory.getCreatedOn());
        assertEquals("email1", operationsHistory.getEmail());
        assertEquals("tenantCode1", operationsHistory.getTenantCode());
        assertEquals("operation1", operationsHistory.getOperation());
        assertEquals("area1", operationsHistory.getArea());
        assertEquals("uploadStatus1", operationsHistory.getUploadStatus());
    }

    @Test
    void testSettersAndGetters() {
        OperationsHistory operationsHistory = new OperationsHistory();
        operationsHistory.setPk("pk2");
        operationsHistory.setSk("sk2");
        operationsHistory.setFileName("fileName2");
        operationsHistory.setUploadedBy("uploadedBy2");
        operationsHistory.setCreatedOn("createdOn2");
        operationsHistory.setEmail("email2");
        operationsHistory.setTenantCode("tenantCode2");
        operationsHistory.setOperation("operation2");
        operationsHistory.setArea("area2");
        operationsHistory.setUploadStatus("uploadStatus2");

        assertEquals("pk2", operationsHistory.getPk());
        assertEquals("sk2", operationsHistory.getSk());
        assertEquals("fileName2", operationsHistory.getFileName());
        assertEquals("uploadedBy2", operationsHistory.getUploadedBy());
        assertEquals("createdOn2", operationsHistory.getCreatedOn());
        assertEquals("email2", operationsHistory.getEmail());
        assertEquals("tenantCode2", operationsHistory.getTenantCode());
        assertEquals("operation2", operationsHistory.getOperation());
        assertEquals("area2", operationsHistory.getArea());
        assertEquals("uploadStatus2", operationsHistory.getUploadStatus());
    }

    @Test
    void testEqualsAndHashCode() {
        OperationsHistory operationsHistory1 = new OperationsHistory(
                "pk1", "sk1", "fileName1", "uploadedBy1", "createdOn1",
                "email1", "tenantCode1", "operation1", "area1","uploadStatus1", "", "", ""
        );
        OperationsHistory operationsHistory2 = new OperationsHistory(
                "pk1", "sk1", "fileName1", "uploadedBy1", "createdOn1",
                "email1", "tenantCode1", "operation1", "area1","uploadStatus1", "", "", ""
        );

        assertEquals(operationsHistory1, operationsHistory2);
        assertEquals(operationsHistory1.hashCode(), operationsHistory2.hashCode());
    }

    @Test
    void testToString() {
        OperationsHistory operationsHistory = new OperationsHistory(
                "pk1", "sk1", "fileName1", "uploadedBy1", "createdOn1",
                "email1", "tenantCode1", "operation1", "area1","uploadStatus1", "adduser.csv","Completed", "100"
        );
        String expected = "OperationsHistory(pk=pk1, sk=sk1, fileName=fileName1, uploadedBy=uploadedBy1, createdOn=createdOn1, email=email1, tenantCode=tenantCode1, operation=operation1, area=area1, uploadStatus=uploadStatus1, uploadedFileS3key=adduser.csv, fileStatus=Completed, fileProgress=100)";
        assertEquals(expected, operationsHistory.toString());
    }
}
package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonUtilTest {


    static class TestDto {
        public String name;
        public int age;
        public boolean active;
    }

    @Test
    void testMapItemToDto_Success() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("name", AttributeValue.builder().s("John").build());
        item.put("age", AttributeValue.builder().n("30").build());
        item.put("active", AttributeValue.builder().bool(true).build());

        TestDto dto = CommonUtil.mapItemToDto(item, TestDto.class);

        assertEquals("John", dto.name);
        assertEquals(30, dto.age);
        assertTrue(dto.active);
    }

    @Test
    void testMapItemToDto_MissingFields() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("name", AttributeValue.builder().s("Alice").build());

        TestDto dto = CommonUtil.mapItemToDto(item, TestDto.class);

        assertEquals("Alice", dto.name);
        assertEquals(0, dto.age); // default int
        assertFalse(dto.active);  // default boolean
    }

    @Test
    void testMapItemToDto_InvalidType() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("age", AttributeValue.builder().s("notANumber").build());

        assertThrows(RuntimeException.class, () -> CommonUtil.mapItemToDto(item, TestDto.class));
    }

    @Test
    void testGenerateUniqueId() {
        String id1 = CommonUtil.generateUniqueId();
        String id2 = CommonUtil.generateUniqueId();

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        assertTrue(id1.length() > 0);
    }
}

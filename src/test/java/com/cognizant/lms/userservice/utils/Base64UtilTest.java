package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Base64UtilTest {
    @Test
    void decodeEvaluatedKey_ValidEncodedKey() {
        String encodedKey = Base64.getEncoder().encodeToString("{key1=value1, key2=value2}".getBytes(StandardCharsets.UTF_8));
        Map<String, String> result = Base64Util.decodeEvaluatedKey(encodedKey);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void decodeEvaluatedKey_InvalidEncodedKey() {
        String encodedKey = "InvalidBase64=String";
        assertThrows(IllegalArgumentException.class, () -> Base64Util.decodeEvaluatedKey(encodedKey));
    }

    @Test
    void encodeLastEvaluatedKey_ValidMap() {
        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("key1", AttributeValue.builder().s("value1").build());
        lastKey.put("key2", AttributeValue.builder().s("value2").build());

        String encodedKey = Base64Util.encodeLastEvaluatedKey(lastKey);
        String expectedEncodedKey = Base64.getEncoder().encodeToString("{key1=value1, key2=value2}".getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedEncodedKey, encodedKey);
    }

    @Test
    void convertAttributeValueMap_ValidMap() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("key1", AttributeValue.builder().s("value1").build());
        attributeValueMap.put("key2", AttributeValue.builder().s("value2").build());

        Map<String, String> result = Base64Util.convertAttributeValueMap(attributeValueMap);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }
}

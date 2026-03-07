package com.cognizant.lms.userservice.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class Base64Util {

  public static Map<String, String> decodeEvaluatedKey(String lastEvaluatedKeyEncoded) {
    String cleanedLastEvaluatedKey = lastEvaluatedKeyEncoded.replace("\"", "");
    byte[] decodedBytes = Base64.getDecoder().decode(cleanedLastEvaluatedKey);
    String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
    return convertStringToMap(decodedString);
  }

  public static String encodeLastEvaluatedKey(Map<String, AttributeValue> lastKey) {
    String lastKeyString = convertAttributeValueMap(lastKey).toString();
    return Base64.getEncoder().encodeToString(lastKeyString.getBytes(StandardCharsets.UTF_8));
  }

  public static Map<String, String> convertAttributeValueMap(Map<String, AttributeValue>
                                                                 attributeValueMap) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, AttributeValue> entry :
        attributeValueMap.entrySet()) {
      result.put(entry.getKey(), entry.getValue().s());
    }
    return result;
  }

  private static Map<String, String> convertStringToMap(String str) {
    Map<String, String> map = new HashMap<>();
    String[] entries = str.substring(1, str.length() - 1).split(", ");
    for (String entry : entries) {
      String[] keyValue = entry.split("=");
      map.put(keyValue[0], keyValue[1]);
    }
    return map;
  }
}


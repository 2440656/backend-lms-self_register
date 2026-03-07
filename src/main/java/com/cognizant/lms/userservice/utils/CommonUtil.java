package com.cognizant.lms.userservice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CommonUtil {

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T mapItemToDto(Map<String, AttributeValue> item, Class<T> type) {
        try {
            T dto = type.getDeclaredConstructor().newInstance();
            Field[] fields = FIELD_CACHE.computeIfAbsent(type, cls -> {
                Field[] declaredFields = cls.getDeclaredFields();
                for (Field field : declaredFields) {
                    field.setAccessible(true);
                }
                return declaredFields;
            });

            for (Field field : fields) {
                AttributeValue attributeValue = item.get(field.getName());
                if (attributeValue != null) {
                    setFieldValue(dto, field, attributeValue);
                }
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Error creating instance of " + type.getName(), e);
        }
    }

    private static void setFieldValue(Object dto, Field field, AttributeValue attributeValue) {
        try {
            if(attributeValue == null) {
                return;
            }
            if (field.getType().equals(boolean.class)) {
                field.set(dto, Boolean.parseBoolean(attributeValue.bool() != null ? attributeValue.bool().toString() : attributeValue.s()));
            } else if (field.getType().equals(int.class)) {
                field.set(dto, Integer.parseInt(attributeValue.n()));
            } else {
                field.set(dto, attributeValue.s());
            }
        } catch (IllegalAccessException e) {
            log.error("Error setting field value for {}: {}", field.getName(), e.getMessage());
        }
    }

    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }
}

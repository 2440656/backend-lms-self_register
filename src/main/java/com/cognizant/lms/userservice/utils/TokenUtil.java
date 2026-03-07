package com.cognizant.lms.userservice.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Autowired;


import com.cognizant.lms.userservice.constants.Constants;

@Slf4j
@Component
public class TokenUtil {

    @Autowired
    private  SanitizeUtil sanitizeUtil;

    public  String getAuthorizationHeader() {
        try {
            HttpServletRequest currentRequest = ((ServletRequestAttributes)
                Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            String authHeader = currentRequest.getHeader(Constants.AUTHORIZATION);

            if (authHeader != null && !authHeader.isEmpty()) {
                // Sanitize the authorization header using existing SanitizeUtil method
                String sanitizedHeader = sanitizeUtil.sanitizeData(authHeader);
                if (sanitizedHeader == null || sanitizedHeader.isEmpty()) {
                    log.warn("Invalid or potentially malicious Authorization header detected");
                    return "";
                }
                return sanitizedHeader;
            }

            log.error("No Authorization header found in the request");
            return "";
        } catch (Exception e) {
            log.error("Error processing Authorization header", e);
            return "";
        }

    }

    public static String extractIssuedAtTimeStamp(String token) {
        String issuedAt = null;
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode payloadNode = objectMapper.readTree(payload);
                if (payloadNode.has("iat")) {
                    long iat = payloadNode.get("iat").asLong();
                    issuedAt = Instant.ofEpochSecond(iat).toString();
                    log.info("Token issued at: {}", issuedAt);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract issuedAt from token: {}", e.getMessage());
        }
        return issuedAt;
    }

    public static String extractSubFromToken(String token) {
        String sub = null;
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode payloadNode = objectMapper.readTree(payload);
                if (payloadNode.has("sub")) {
                    sub = payloadNode.get("sub").asText();
                    log.info("Token subject (sub): {}", sub);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract sub from token: {}", e.getMessage());
        }
        return sub;
    }
}

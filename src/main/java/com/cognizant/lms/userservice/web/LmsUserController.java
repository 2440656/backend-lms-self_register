package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.TenantUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/lmsusers")
@Slf4j
public class LmsUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/userByEmail/{email}")
    public ResponseEntity<HttpResponse> getUserDetailsByEmailId(@PathVariable(name = "email") String email,
                                                                @RequestParam(value = "status",
                                                                        required = false) String status) {
        HttpResponse response = new HttpResponse();
        User user = userService.getUserByEmailId(email, status);
        if (user == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            response.setError("User not found with emailId: " + email);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            user.setTenant(TenantUtil.getTenantDetails());
            response.setData(user);
            response.setStatus(HttpStatus.OK.value());
            response.setError(null);
            log.info("Fetching user {} by emailId", user.getPk());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get user counts by tenant codes in a single call.
     *
     * Usage examples:
     * - /tenantUsersCount?tenantCodes=t-1,t-2,t-3
     * - /tenantUsersCount?tenantCodes=t-1&tenantCodes=t-2
     *
     * @param tenantCodes one or more tenant codes
     * @return Map with tenantCodes and their respective user counts
     */
    @PostMapping("/tenantUsersCount")
    public ResponseEntity<Map<String, Object>> getUserCountByTenantCode(
            @RequestBody(required = false) List<String> tenantCodes) {
        try {
            List<String> normalizedTenantCodes = tenantCodes == null ? List.of() : tenantCodes.stream()
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .flatMap(code -> Arrays.stream(code.split(",")))
                    .map(String::trim)
                    .filter(code -> !code.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            log.info("Fetching user counts for tenant codes: {}", normalizedTenantCodes);

            Map<String, Integer> counts = userService.getUserCountByTenantCodes(normalizedTenantCodes);

            Map<String, Object> response = new HashMap<>();
            response.put("tenantCodes", normalizedTenantCodes);
            response.put("counts", counts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user counts for tenant codes {}: {}", tenantCodes, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch user counts");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}

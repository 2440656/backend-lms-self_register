package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.dto.TenantDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TenantUtilTest {
    @Test
    void getTenantCode_returnsCorrectTenantCode() {
        TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
        String tenantCode = TenantUtil.getTenantCode();
        assertEquals("t-2", tenantCode);
    }
}

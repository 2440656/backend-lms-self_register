package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class TenantDTO {
    private String pk;
    private String tenantIdentifier;
    private String name;
    private String idpPreferences;
    private String portal;
    private String clientId;
    private String issuer;
    private String certUrl;
}

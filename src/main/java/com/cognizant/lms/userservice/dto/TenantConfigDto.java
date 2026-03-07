package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigDto {
    private String adminURL;
    private String appClientId;
    private String awsCertUrl;
    private String awsCognitoIssuer;
    private String cookieDomain;
    private String host;
    private String hostedUiDomain;
    private String hostedUiLoginUrl;
    private String learnerCatalogURL;
    private String learnerURL;
    private String mainURL;
    private String name;
    private String userPoolAppId;
    private String userPoolAppSecret;
    private String userPoolDomain;
    private String userPoolId;
    private String superAdmin;
    private String tenantAdmin;
    private String siteLogoPath;
    private String userMediaURL;
    private String mediaURL;
    private String logoutURL;
    private List<LanguageDto> language;
    private String tenantServiceURL;
    private Boolean betaEnabled;

}

package com.cognizant.lms.userservice.utils;
import com.cognizant.lms.userservice.dto.TenantDTO;

public class TenantUtil {
  private static TenantDTO tenant;

  public static void setTenantDetails(TenantDTO tenantDTO) {
    tenant = tenantDTO;
  }

  public static String getTenantCode() {
    if(tenant == null) {
        throw new IllegalStateException("Tenant details are not set");
    }
    return tenant.getPk();
  }

  public static String getPortal() {
    if(tenant == null) {
        throw new IllegalStateException("Tenant details are not set");
    }
    return tenant.getPortal();
  }

  public static TenantDTO getTenantDetails() {
    if(tenant == null) {
        throw new IllegalStateException("Tenant details are not set");
    }
    return tenant;
  }

  public static String getClientId() {
    if(tenant == null) {
        throw new IllegalStateException("Tenant details are not set");
    }
    return tenant.getClientId();
  }

    public static String getIssuer() {
        if(tenant == null) {
            throw new IllegalStateException("Tenant details are not set");
        }
        return tenant.getIssuer();
    }

    public static String getCertUrl() {
        if(tenant == null) {
            throw new IllegalStateException("Tenant details are not set");
        }
        return tenant.getCertUrl();
    }
}

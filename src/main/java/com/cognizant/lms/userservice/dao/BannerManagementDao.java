package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.Tenant;

import java.util.List;

public interface BannerManagementDao {
    void saveBannerManagementIcon(Tenant tenantBannerManagement);

    int countActiveBanners(String tenantCode);

    Tenant getBannerById(String tenantCode, String bannerId);

    List<Tenant> getBannersByTenant(String tenantCode);

    void deleteBannerById(String tenantCode, String bannerId);
}

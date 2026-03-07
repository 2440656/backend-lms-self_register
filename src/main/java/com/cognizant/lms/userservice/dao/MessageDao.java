package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.Tenant;

import java.util.List;

public interface MessageDao {
    Tenant findTenantByKey(String tenantCode, String category);
    void saveTenant(Tenant tenant);
    List<Tenant> findTenantByCategoryAndStatus(String tenantCode, String category, Boolean status);
}


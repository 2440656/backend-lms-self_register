package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.Tenant;
import java.util.List;

public interface QuickLinkDao {
    void saveQuickLink(Tenant tenantQuickLink);

    Tenant updateQuickLink(Tenant tenantQuickLink);

    void deleteQuickLink(String pk, String sk);

    Tenant getQuickLinkByPk(String pk, String sk);

    void updateQuickLinksTransactional(List<Tenant> links);

    List<Tenant> getQuickLinksByTenant(String tenantCode);
}

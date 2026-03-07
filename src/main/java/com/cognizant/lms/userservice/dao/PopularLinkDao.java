package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.Tenant;
import java.util.List;

public interface PopularLinkDao {
    void savePopularLink(Tenant tenantPopularLink);

    Tenant updatePopularLink(Tenant tenantPopularLink);

    void deletePopularLink(String pk, String sk);

    Tenant getPopularLinkByPk(String pk, String sk);

    void updatePopularLinksTransactional(List<Tenant> links);

    List<Tenant> getPopularLinksByTenant(String tenantCode);

}

package com.cognizant.lms.userservice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantTest {

    @Test
    void testNoArgsConstructor() {
        Tenant tenant = new Tenant();
        assertNull(tenant.getPk());
        assertNull(tenant.getSk());
        assertNull(tenant.getType());
        assertNull(tenant.getName());
        assertNull(tenant.getIdpPreferences());
        assertNull(tenant.getSettings());
        assertNull(tenant.getCreatedOn());
        assertNull(tenant.getStatus());
        assertNull(tenant.getTenantIdentifier());
        assertNull(tenant.getUpdatedBy());
        assertNull(tenant.getCreatedBy());
        assertNull(tenant.getReviewEmail());
        assertNull(tenant.getCourseReviewCommentType());
        assertNull(tenant.getUpdatedDate());
        assertNull(tenant.getSettingName());
        assertNull(tenant.getCategory());
        assertNull(tenant.getMessage());
        assertNull(tenant.getMessageStatus());
        assertNull(tenant.getLmsIntegrationId());
        assertNull(tenant.getProvider());
        assertNull(tenant.getIntegrationType());
        assertNull(tenant.getIntegrationId());
        assertNull(tenant.getIntegrationOwner());
        assertNull(tenant.getHostName());
        assertNull(tenant.getClientId());
        assertNull(tenant.getClientSecret());
        assertNull(tenant.getOrganizationId());
        assertNull(tenant.getFieldName());
        assertNull(tenant.getFieldValue());
        assertNull(tenant.getLinkId());
        assertNull(tenant.getTitle());
        assertNull(tenant.getUrl());
        assertNull(tenant.getDescription());
        assertNull(tenant.getAction());
        assertNull(tenant.getModifiedOn());
        assertNull(tenant.getModifiedBy());
        assertNull(tenant.getIndex());
        assertNull(tenant.getIconKey());
        assertNull(tenant.getIconFileName());
        assertNull(tenant.getBannerId());
        assertNull(tenant.getBannerTitle());
        assertNull(tenant.getBannerDescription());
        assertNull(tenant.getBannerStatus());
        assertNull(tenant.getStartDate());
        assertNull(tenant.getEndDate());
        assertNull(tenant.getBannerHeading());
        assertNull(tenant.getBannerSubHeading());
        assertNull(tenant.getBannerRedirectionUrl());
        assertNull(tenant.getBannerImageKey());
    }

    @Test
    void testAllArgsConstructor() {
        Tenant tenant = new Tenant(
                "pk1", "sk1", "type1", "name1", "idpPreferences1", "settings1",
                "createdOn1", "status1", "tenantIdentifier1", "updatedBy1",
                "createdBy1", "reviewEmail1", "courseReviewCommentType1",
                "updatedDate1", "settingName1", "category1", "message1", true,
                "lmsIntegrationId1", "provider1", "integrationType1", "integrationId1",
                "integrationOwner1", "hostName1", "clientId1", "clientSecret1",
                "organizationId1", "fieldName1", "fieldValue1", "linkId1", "title1",
                "url1", "desc1", "action1", "2025-09-19", "user1", 5,
                "iconKey1", "iconFileName1", "banner-001", "Title", "Description", "ACTIVE",
                "2025-11-01", "2025-11-30", "Heading", "SubHeading", "https://example.com", "image-key-001"
        );

        assertEquals("pk1", tenant.getPk());
        assertEquals("sk1", tenant.getSk());
        assertEquals("type1", tenant.getType());
        assertEquals("name1", tenant.getName());
        assertEquals("idpPreferences1", tenant.getIdpPreferences());
        assertEquals("settings1", tenant.getSettings());
        assertEquals("createdOn1", tenant.getCreatedOn());
        assertEquals("status1", tenant.getStatus());
        assertEquals("tenantIdentifier1", tenant.getTenantIdentifier());
        assertEquals("updatedBy1", tenant.getUpdatedBy());
        assertEquals("createdBy1", tenant.getCreatedBy());
        assertEquals("reviewEmail1", tenant.getReviewEmail());
        assertEquals("courseReviewCommentType1", tenant.getCourseReviewCommentType());
        assertEquals("updatedDate1", tenant.getUpdatedDate());
        assertEquals("settingName1", tenant.getSettingName());
        assertEquals("category1", tenant.getCategory());
        assertEquals("message1", tenant.getMessage());
        assertEquals(true, tenant.getMessageStatus());
        assertEquals("lmsIntegrationId1", tenant.getLmsIntegrationId());
        assertEquals("provider1", tenant.getProvider());
        assertEquals("integrationType1", tenant.getIntegrationType());
        assertEquals("integrationId1", tenant.getIntegrationId());
        assertEquals("integrationOwner1", tenant.getIntegrationOwner());
        assertEquals("hostName1", tenant.getHostName());
        assertEquals("clientId1", tenant.getClientId());
        assertEquals("clientSecret1", tenant.getClientSecret());
        assertEquals("organizationId1", tenant.getOrganizationId());
        assertEquals("fieldName1", tenant.getFieldName());
        assertEquals("fieldValue1", tenant.getFieldValue());
        assertEquals("linkId1", tenant.getLinkId());
        assertEquals("title1", tenant.getTitle());
        assertEquals("url1", tenant.getUrl());
        assertEquals("desc1", tenant.getDescription());
        assertEquals("action1", tenant.getAction());
        assertEquals("2025-09-19", tenant.getModifiedOn());
        assertEquals("user1", tenant.getModifiedBy());
        assertEquals(5, tenant.getIndex());
        assertEquals("iconKey1", tenant.getIconKey());
        assertEquals("iconFileName1", tenant.getIconFileName());
        assertEquals("banner-001", tenant.getBannerId());
        assertEquals("Title", tenant.getBannerTitle());
        assertEquals("Description", tenant.getBannerDescription());
        assertEquals("ACTIVE", tenant.getBannerStatus());
        assertEquals("2025-11-01", tenant.getStartDate());
        assertEquals("2025-11-30", tenant.getEndDate());
        assertEquals("Heading", tenant.getBannerHeading());
        assertEquals("SubHeading", tenant.getBannerSubHeading());
        assertEquals("https://example.com", tenant.getBannerRedirectionUrl());
        assertEquals("image-key-001", tenant.getBannerImageKey());
    }

    @Test
    void testSettersAndGetters() {
        Tenant tenant = new Tenant();
        tenant.setPk("pk2");
        tenant.setSk("sk2");
        tenant.setType("type2");
        tenant.setName("name2");
        tenant.setIdpPreferences("idpPreferences2");
        tenant.setSettings("settings2");
        tenant.setCreatedOn("createdOn2");
        tenant.setStatus("status2");
        tenant.setTenantIdentifier("tenantIdentifier2");
        tenant.setUpdatedBy("updatedBy2");
        tenant.setCreatedBy("createdBy2");
        tenant.setReviewEmail("reviewEmail2");
        tenant.setCourseReviewCommentType("courseReviewCommentType2");
        tenant.setUpdatedDate("updatedDate2");
        tenant.setSettingName("settingName2");
        tenant.setCategory("category2");
        tenant.setMessage("message2");
        tenant.setMessageStatus(false);
        tenant.setLmsIntegrationId("lmsIntegrationId2");
        tenant.setProvider("provider2");
        tenant.setIntegrationType("integrationType2");
        tenant.setIntegrationId("integrationId2");
        tenant.setIntegrationOwner("integrationOwner2");
        tenant.setHostName("hostName2");
        tenant.setClientId("clientId2");
        tenant.setClientSecret("clientSecret2");
        tenant.setOrganizationId("organizationId2");
        tenant.setFieldName("fieldName2");
        tenant.setFieldValue("fieldValue2");
        tenant.setLinkId("linkId2");
        tenant.setTitle("title2");
        tenant.setUrl("url2");
        tenant.setDescription("desc2");
        tenant.setAction("action2");
        tenant.setModifiedOn("2025-09-20");
        tenant.setModifiedBy("user2");
        tenant.setIndex(10);
        tenant.setIconKey("iconKey2");
        tenant.setIconFileName("iconFileName2");
        tenant.setBannerId("banner-002");
        tenant.setBannerTitle("New Title");
        tenant.setBannerDescription("New Description");
        tenant.setBannerStatus("INACTIVE");
        tenant.setStartDate("2025-12-01");
        tenant.setEndDate("2025-12-31");
        tenant.setBannerHeading("New Heading");
        tenant.setBannerSubHeading("New SubHeading");
        tenant.setBannerRedirectionUrl("https://example.org");
        tenant.setBannerImageKey("image-key-002");

        assertEquals("pk2", tenant.getPk());
        assertEquals("sk2", tenant.getSk());
        assertEquals("type2", tenant.getType());
        assertEquals("name2", tenant.getName());
        assertEquals("idpPreferences2", tenant.getIdpPreferences());
        assertEquals("settings2", tenant.getSettings());
        assertEquals("createdOn2", tenant.getCreatedOn());
        assertEquals("status2", tenant.getStatus());
        assertEquals("tenantIdentifier2", tenant.getTenantIdentifier());
        assertEquals("updatedBy2", tenant.getUpdatedBy());
        assertEquals("createdBy2", tenant.getCreatedBy());
        assertEquals("reviewEmail2", tenant.getReviewEmail());
        assertEquals("courseReviewCommentType2", tenant.getCourseReviewCommentType());
        assertEquals("updatedDate2", tenant.getUpdatedDate());
        assertEquals("settingName2", tenant.getSettingName());
        assertEquals("category2", tenant.getCategory());
        assertEquals("message2", tenant.getMessage());
        assertEquals(false, tenant.getMessageStatus());
        assertEquals("lmsIntegrationId2", tenant.getLmsIntegrationId());
        assertEquals("provider2", tenant.getProvider());
        assertEquals("integrationType2", tenant.getIntegrationType());
        assertEquals("integrationId2", tenant.getIntegrationId());
        assertEquals("integrationOwner2", tenant.getIntegrationOwner());
        assertEquals("hostName2", tenant.getHostName());
        assertEquals("clientId2", tenant.getClientId());
        assertEquals("clientSecret2", tenant.getClientSecret());
        assertEquals("organizationId2", tenant.getOrganizationId());
        assertEquals("fieldName2", tenant.getFieldName());
        assertEquals("fieldValue2", tenant.getFieldValue());
        assertEquals("linkId2", tenant.getLinkId());
        assertEquals("title2", tenant.getTitle());
        assertEquals("url2", tenant.getUrl());
        assertEquals("desc2", tenant.getDescription());
        assertEquals("action2", tenant.getAction());
        assertEquals("2025-09-20", tenant.getModifiedOn());
        assertEquals("user2", tenant.getModifiedBy());
        assertEquals(10, tenant.getIndex());
        assertEquals("iconKey2", tenant.getIconKey());
        assertEquals("iconFileName2", tenant.getIconFileName());
        assertEquals("banner-002", tenant.getBannerId());
        assertEquals("New Title", tenant.getBannerTitle());
        assertEquals("New Description", tenant.getBannerDescription());
        assertEquals("INACTIVE", tenant.getBannerStatus());
        assertEquals("2025-12-01", tenant.getStartDate());
        assertEquals("2025-12-31", tenant.getEndDate());
        assertEquals("New Heading", tenant.getBannerHeading());
        assertEquals("New SubHeading", tenant.getBannerSubHeading());
        assertEquals("https://example.org", tenant.getBannerRedirectionUrl());
        assertEquals("image-key-002", tenant.getBannerImageKey());
    }

    @Test
    void testEqualsAndHashCode() {
        Tenant tenant1 = new Tenant();
        Tenant tenant2 = new Tenant();
        // Set all fields to same values
        tenant1.setPk("pk1");
        tenant1.setSk("sk1");
        tenant1.setType("type1");
        tenant1.setName("name1");
        tenant1.setIdpPreferences("idpPreferences1");
        tenant1.setSettings("settings1");
        tenant1.setCreatedOn("createdOn1");
        tenant1.setStatus("status1");
        tenant1.setTenantIdentifier("tenantIdentifier1");
        tenant1.setUpdatedBy("updatedBy1");
        tenant1.setCreatedBy("createdBy1");
        tenant1.setReviewEmail("reviewEmail1");
        tenant1.setCourseReviewCommentType("courseReviewCommentType1");
        tenant1.setUpdatedDate("updatedDate1");
        tenant1.setSettingName("settingName1");
        tenant1.setCategory("category1");
        tenant1.setMessage("message1");
        tenant1.setMessageStatus(true);
        tenant1.setLmsIntegrationId("lmsIntegrationId1");
        tenant1.setProvider("provider1");
        tenant1.setIntegrationType("integrationType1");
        tenant1.setIntegrationId("integrationId1");
        tenant1.setIntegrationOwner("integrationOwner1");
        tenant1.setHostName("hostName1");
        tenant1.setClientId("clientId1");
        tenant1.setClientSecret("clientSecret1");
        tenant1.setOrganizationId("organizationId1");
        tenant1.setFieldName("fieldName1");
        tenant1.setFieldValue("fieldValue1");
        tenant1.setLinkId("linkId1");
        tenant1.setTitle("title1");
        tenant1.setUrl("url1");
        tenant1.setDescription("desc1");
        tenant1.setAction("action1");
        tenant1.setModifiedOn("2025-09-19");
        tenant1.setModifiedBy("user1");
        tenant1.setIndex(1);
        tenant1.setIconKey("iconKey1");
        tenant1.setIconFileName("iconFileName1");
        tenant1.setBannerId("banner-001");
        tenant1.setBannerTitle("Title");
        tenant1.setBannerDescription("Description");
        tenant1.setBannerStatus("ACTIVE");
        tenant1.setStartDate("2025-11-01");
        tenant1.setEndDate("2025-11-30");
        tenant1.setBannerHeading("Heading");
        tenant1.setBannerSubHeading("SubHeading");
        tenant1.setBannerRedirectionUrl("https://example.com");
        tenant1.setBannerImageKey("image-key-001");
        // Set same values for tenant2
        tenant2.setPk("pk1");
        tenant2.setSk("sk1");
        tenant2.setType("type1");
        tenant2.setName("name1");
        tenant2.setIdpPreferences("idpPreferences1");
        tenant2.setSettings("settings1");
        tenant2.setCreatedOn("createdOn1");
        tenant2.setStatus("status1");
        tenant2.setTenantIdentifier("tenantIdentifier1");
        tenant2.setUpdatedBy("updatedBy1");
        tenant2.setCreatedBy("createdBy1");
        tenant2.setReviewEmail("reviewEmail1");
        tenant2.setCourseReviewCommentType("courseReviewCommentType1");
        tenant2.setUpdatedDate("updatedDate1");
        tenant2.setSettingName("settingName1");
        tenant2.setCategory("category1");
        tenant2.setMessage("message1");
        tenant2.setMessageStatus(true);
        tenant2.setLmsIntegrationId("lmsIntegrationId1");
        tenant2.setProvider("provider1");
        tenant2.setIntegrationType("integrationType1");
        tenant2.setIntegrationId("integrationId1");
        tenant2.setIntegrationOwner("integrationOwner1");
        tenant2.setHostName("hostName1");
        tenant2.setClientId("clientId1");
        tenant2.setClientSecret("clientSecret1");
        tenant2.setOrganizationId("organizationId1");
        tenant2.setFieldName("fieldName1");
        tenant2.setFieldValue("fieldValue1");
        tenant2.setLinkId("linkId1");
        tenant2.setTitle("title1");
        tenant2.setUrl("url1");
        tenant2.setDescription("desc1");
        tenant2.setAction("action1");
        tenant2.setModifiedOn("2025-09-19");
        tenant2.setModifiedBy("user1");
        tenant2.setIndex(1);
        tenant2.setIconKey("iconKey1");
        tenant2.setIconFileName("iconFileName1");
        tenant2.setBannerId("banner-001");
        tenant2.setBannerTitle("Title");
        tenant2.setBannerDescription("Description");
        tenant2.setBannerStatus("ACTIVE");
        tenant2.setStartDate("2025-11-01");
        tenant2.setEndDate("2025-11-30");
        tenant2.setBannerHeading("Heading");
        tenant2.setBannerSubHeading("SubHeading");
        tenant2.setBannerRedirectionUrl("https://example.com");
        tenant2.setBannerImageKey("image-key-001");
        assertEquals(tenant1, tenant2);
        assertEquals(tenant1.hashCode(), tenant2.hashCode());
    }

    @Test
    void testToString() {
        Tenant tenant = new Tenant();
        tenant.setPk("pk1");
        tenant.setSk("sk1");
        tenant.setType("type1");
        tenant.setName("name1");
        tenant.setIdpPreferences("idpPreferences1");
        tenant.setSettings("settings1");
        tenant.setCreatedOn("createdOn1");
        tenant.setStatus("status1");
        tenant.setTenantIdentifier("tenantIdentifier1");
        tenant.setUpdatedBy("updatedBy1");
        tenant.setCreatedBy("createdBy1");
        tenant.setReviewEmail("reviewEmail1");
        tenant.setCourseReviewCommentType("courseReviewCommentType1");
        tenant.setUpdatedDate("updatedDate1");
        tenant.setSettingName("settingName1");
        tenant.setCategory("category1");
        tenant.setMessage("message1");
        tenant.setMessageStatus(true);
        tenant.setLmsIntegrationId("lmsIntegrationId1");
        tenant.setProvider("provider1");
        tenant.setIntegrationType("integrationType1");
        tenant.setIntegrationId("integrationId1");
        tenant.setIntegrationOwner("integrationOwner1");
        tenant.setHostName("hostName1");
        tenant.setClientId("clientId1");
        tenant.setClientSecret("clientSecret1");
        tenant.setOrganizationId("organizationId1");
        tenant.setFieldName("fieldName1");
        tenant.setFieldValue("fieldValue1");
        tenant.setLinkId("linkId1");
        tenant.setTitle("title1");
        tenant.setUrl("url1");
        tenant.setDescription("desc1");
        tenant.setAction("action1");
        tenant.setModifiedOn("2025-09-19");
        tenant.setModifiedBy("user1");
        tenant.setIndex(1);
        tenant.setIconKey("iconKey1");
        tenant.setIconFileName("iconFileName1");
        tenant.setBannerId("banner-001");
        tenant.setBannerTitle("Title");
        tenant.setBannerDescription("Description");
        tenant.setBannerStatus("ACTIVE");
        tenant.setStartDate("2025-11-01");
        tenant.setEndDate("2025-11-30");
        tenant.setBannerHeading("Heading");
        tenant.setBannerSubHeading("SubHeading");
        tenant.setBannerRedirectionUrl("https://example.com");
        tenant.setBannerImageKey("image-key-001");
        String expected = tenant.toString();
        assertEquals(expected, tenant.toString());
    }

    @Test
    void testLinkFields() {
        Tenant tenant = new Tenant();
        // Initially, all should be null
        assertNull(tenant.getLinkId());
        assertNull(tenant.getTitle());
        assertNull(tenant.getUrl());
        assertNull(tenant.getDescription());
        assertNull(tenant.getAction());
        assertNull(tenant.getModifiedOn());
        assertNull(tenant.getModifiedBy());
        assertNull(tenant.getIndex());
        assertNull(tenant.getIconKey());
        assertNull(tenant.getIconFileName());

        // Set values
        tenant.setLinkId("linkId1");
        tenant.setTitle("title1");
        tenant.setUrl("url1");
        tenant.setDescription("desc1");
        tenant.setAction("action1");
        tenant.setModifiedOn("2025-09-19");
        tenant.setModifiedBy("user1");
        tenant.setIndex(5);
        tenant.setIconKey("iconKey1");
        tenant.setIconFileName("iconFileName1");

        // Assert values
        assertEquals("linkId1", tenant.getLinkId());
        assertEquals("title1", tenant.getTitle());
        assertEquals("url1", tenant.getUrl());
        assertEquals("desc1", tenant.getDescription());
        assertEquals("action1", tenant.getAction());
        assertEquals("2025-09-19", tenant.getModifiedOn());
        assertEquals("user1", tenant.getModifiedBy());
        assertEquals(5, tenant.getIndex());
        assertEquals("iconKey1", tenant.getIconKey());
        assertEquals("iconFileName1", tenant.getIconFileName());
    }

    @Test
    void testBannerManagementFields() {
        Tenant tenant = new Tenant();
        // Initially, all should be null
        assertNull(tenant.getBannerId());
        assertNull(tenant.getBannerTitle());
        assertNull(tenant.getBannerDescription());
        assertNull(tenant.getBannerStatus());
        assertNull(tenant.getStartDate());
        assertNull(tenant.getEndDate());
        assertNull(tenant.getBannerHeading());
        assertNull(tenant.getBannerSubHeading());
        assertNull(tenant.getBannerRedirectionUrl());
        assertNull(tenant.getBannerImageKey());

        // Set values
        tenant.setBannerId("banner-001");
        tenant.setBannerTitle("Title");
        tenant.setBannerDescription("Description");
        tenant.setBannerStatus("ACTIVE");
        tenant.setStartDate("2025-11-01");
        tenant.setEndDate("2025-11-30");
        tenant.setBannerHeading("Heading");
        tenant.setBannerSubHeading("SubHeading");
        tenant.setBannerRedirectionUrl("https://example.com");
        tenant.setBannerImageKey("image-key-001");

        // Assert values
        assertEquals("banner-001", tenant.getBannerId());
        assertEquals("Title", tenant.getBannerTitle());
        assertEquals("Description", tenant.getBannerDescription());
        assertEquals("ACTIVE", tenant.getBannerStatus());
        assertEquals("2025-11-01", tenant.getStartDate());
        assertEquals("2025-11-30", tenant.getEndDate());
        assertEquals("Heading", tenant.getBannerHeading());
        assertEquals("SubHeading", tenant.getBannerSubHeading());
        assertEquals("https://example.com", tenant.getBannerRedirectionUrl());
        assertEquals("image-key-001", tenant.getBannerImageKey());
    }
}

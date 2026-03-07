package com.cognizant.lms.userservice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillLookupsTest {

    @Test
    void testNoArgsConstructor() {
        SkillLookups skillLookups = new SkillLookups();
        assertNull(skillLookups.getPk());
        assertNull(skillLookups.getSk());
        assertNull(skillLookups.getType());
        assertNull(skillLookups.getName());
        assertNull(skillLookups.getTenantType());
        assertNull(skillLookups.getNormalizedName());
        assertNull(skillLookups.getNormalizedCode());
        assertNull(skillLookups.getActive());
        assertNull(skillLookups.getGsiTypeSk());
        assertNull(skillLookups.getEffectiveDate());
        assertNull(skillLookups.getSkillCode());
        assertNull(skillLookups.getSkillName());
        assertNull(skillLookups.getSkillDescription());
        assertNull(skillLookups.getSkillType());
        assertNull(skillLookups.getStatus());
        assertNull(skillLookups.getSkillCategory());
        assertNull(skillLookups.getSkillSubCategory());
    }

    @Test
    void testAllArgsConstructor() {
        SkillLookups skillLookups = SkillLookups.builder()
            .pk("pk1")
            .sk("sk1")
            .type("type1")
            .name("name1")
            .tenantType("tenantType1")
            .normalizedName("name1")
            .normalizedCode("code1")
            .active("true")
            .gsiTypeSk("gsiTypeSk1")
            .effectiveDate("2023-10-10")
            .skillCode("skillCode1")
            .skillName("skillName1")
            .skillDescription("skillDescription1")
            .skillType("skillType1")
            .status("status1")
            .skillCategory("skillCategory1")
            .skillSubCategory("skillSubCategory1")
            .build();

        assertEquals("pk1", skillLookups.getPk());
        assertEquals("sk1", skillLookups.getSk());
        assertEquals("type1", skillLookups.getType());
        assertEquals("name1", skillLookups.getName());
        assertEquals("tenantType1", skillLookups.getTenantType());
        assertEquals("name1", skillLookups.getNormalizedName());
        assertEquals("code1", skillLookups.getNormalizedCode());
        assertEquals("true", skillLookups.getActive());
        assertEquals("gsiTypeSk1", skillLookups.getGsiTypeSk());
        assertEquals("2023-10-10", skillLookups.getEffectiveDate());
        assertEquals("skillCode1", skillLookups.getSkillCode());
        assertEquals("skillName1", skillLookups.getSkillName());
        assertEquals("skillDescription1", skillLookups.getSkillDescription());
        assertEquals("skillType1", skillLookups.getSkillType());
        assertEquals("status1", skillLookups.getStatus());
        assertEquals("skillCategory1", skillLookups.getSkillCategory());
        assertEquals("skillSubCategory1", skillLookups.getSkillSubCategory());
    }

    @Test
    void testSettersAndGetters() {
        SkillLookups skillLookups = new SkillLookups();
        skillLookups.setPk("pk2");
        skillLookups.setSk("sk2");
        skillLookups.setType("type2");
        skillLookups.setName("name2");
        skillLookups.setTenantType("tenantType2");
        skillLookups.setNormalizedName("name2");
        skillLookups.setNormalizedCode("code2");
        skillLookups.setActive("false");
        skillLookups.setGsiTypeSk("gsiTypeSk2");
        skillLookups.setEffectiveDate("2023-10-10");
        skillLookups.setSkillCode("skillCode2");
        skillLookups.setSkillName("skillName2");
        skillLookups.setSkillDescription("skillDescription2");
        skillLookups.setSkillType("skillType2");
        skillLookups.setStatus("status2");
        skillLookups.setSkillCategory("skillCategory2");
        skillLookups.setSkillSubCategory("skillSubCategory2");

        assertEquals("pk2", skillLookups.getPk());
        assertEquals("sk2", skillLookups.getSk());
        assertEquals("type2", skillLookups.getType());
        assertEquals("name2", skillLookups.getName());
        assertEquals("tenantType2", skillLookups.getTenantType());
        assertEquals("name2", skillLookups.getNormalizedName());
        assertEquals("code2", skillLookups.getNormalizedCode());
        assertEquals("false", skillLookups.getActive());
        assertEquals("gsiTypeSk2", skillLookups.getGsiTypeSk());
        assertEquals("2023-10-10", skillLookups.getEffectiveDate());
        assertEquals("skillCode2", skillLookups.getSkillCode());
        assertEquals("skillName2", skillLookups.getSkillName());
        assertEquals("skillDescription2", skillLookups.getSkillDescription());
        assertEquals("skillType2", skillLookups.getSkillType());
        assertEquals("status2", skillLookups.getStatus());
        assertEquals("skillCategory2", skillLookups.getSkillCategory());
        assertEquals("skillSubCategory2", skillLookups.getSkillSubCategory());
    }

    @Test
    void testEqualsAndHashCode() {
        SkillLookups skillLookups1 = SkillLookups.builder()
            .pk("pk1")
            .sk("sk1")
            .type("type1")
            .name("name1")
            .tenantType("tenantType1")
            .normalizedName("name1")
            .normalizedCode("code1")
            .active("true")
            .gsiTypeSk("gsiTypeSk1")
            .effectiveDate("2023-10-10")
            .skillCode("skillCode1")
            .skillName("skillName1")
            .skillDescription("skillDescription1")
            .skillType("skillType1")
            .status("status1")
            .skillCategory("skillCategory1")
            .skillSubCategory("skillSubCategory1")
            .build();
        SkillLookups skillLookups2 = SkillLookups.builder()
            .pk("pk1")
            .sk("sk1")
            .type("type1")
            .name("name1")
            .tenantType("tenantType1")
            .normalizedName("name1")
            .normalizedCode("code1")
            .active("true")
            .gsiTypeSk("gsiTypeSk1")
            .effectiveDate("2023-10-10")
            .skillCode("skillCode1")
            .skillName("skillName1")
            .skillDescription("skillDescription1")
            .skillType("skillType1")
            .status("status1")
            .skillCategory("skillCategory1")
            .skillSubCategory("skillSubCategory1")
            .build();

        assertEquals(skillLookups1, skillLookups2);
        assertEquals(skillLookups1.hashCode(), skillLookups2.hashCode());
    }

    @Test
    void testToString() {
        SkillLookups skillLookups = SkillLookups.builder()
            .pk("pk1")
            .sk("sk1")
            .type("type1")
            .name("name1")
            .tenantType("tenantType1")
            .normalizedName("name1")
            .normalizedCode("code1")
            .active("true")
            .gsiTypeSk("gsiTypeSk1")
            .effectiveDate("2023-10-10")
            .skillCode("skillCode1")
            .skillName("skillName1")
            .skillDescription("skillDescription1")
            .skillType("skillType1")
            .status("status1")
            .skillCategory("skillCategory1")
            .skillSubCategory("skillSubCategory1")
            .build();
        String expected = "SkillLookups(pk=pk1, sk=sk1, type=type1, name=name1, tenantType=tenantType1, normalizedName=name1, normalizedCode=code1, active=true, gsiTypeSk=gsiTypeSk1, effectiveDate=2023-10-10, skillCode=skillCode1, skillName=skillName1, skillDescription=skillDescription1, skillType=skillType1, status=status1, skillCategory=skillCategory1, skillSubCategory=skillSubCategory1)";
        assertEquals(expected, skillLookups.toString());
    }
}
package com.cognizant.lms.userservice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LookupTest {

    @Test
    void testNoArgsConstructor() {
        Lookup lookup = new Lookup();
        assertNull(lookup.getPk());
        assertNull(lookup.getSk());
        assertNull(lookup.getName());
        assertNull(lookup.getCode());
        assertNull(lookup.getType());
        assertNull(lookup.getActive());
        assertNull(lookup.getCreatedOn());
    }

    @Test
    void testAllArgsConstructor() {
        Lookup lookup = new Lookup("pk1", "sk1", "CountryName", "Code1", "Type1", "true", "2023-10-01");

        assertEquals("pk1", lookup.getPk());
        assertEquals("sk1", lookup.getSk());
        assertEquals("CountryName", lookup.getName());
        assertEquals("Code1", lookup.getCode());
        assertEquals("Type1", lookup.getType());
        assertEquals("true", lookup.getActive());
        assertEquals("2023-10-01", lookup.getCreatedOn());
    }

    @Test
    void testSettersAndGetters() {
        Lookup lookup = new Lookup();
        lookup.setPk("pk2");
        lookup.setSk("sk2");
        lookup.setName("CountryName2");
        lookup.setCode("Code2");
        lookup.setType("Type2");
        lookup.setActive("false");
        lookup.setCreatedOn("2023-10-02");

        assertEquals("pk2", lookup.getPk());
        assertEquals("sk2", lookup.getSk());
        assertEquals("CountryName2", lookup.getName());
        assertEquals("Code2", lookup.getCode());
        assertEquals("Type2", lookup.getType());
        assertEquals("false", lookup.getActive());
        assertEquals("2023-10-02", lookup.getCreatedOn());
    }

    @Test
    void testEqualsAndHashCode() {
        Lookup lookup1 = new Lookup("pk1", "sk1", "CountryName", "Code1", "Type1", "true", "2023-10-01");
        Lookup lookup2 = new Lookup("pk1", "sk1", "CountryName", "Code1", "Type1", "true", "2023-10-01");

        assertEquals(lookup1, lookup2);
        assertEquals(lookup1.hashCode(), lookup2.hashCode());
    }

    @Test
    void testToString() {
        Lookup lookup = new Lookup("pk1", "sk1", "CountryName", "Code1", "Type1", "true", "2023-10-01");
        String expected = "Lookup(pk=pk1, sk=sk1, name=CountryName, code=Code1, type=Type1, active=true, createdOn=2023-10-01)";
        assertEquals(expected, lookup.toString());
    }
}
package com.cognizant.lms.userservice.domain;

import com.cognizant.lms.userservice.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoleTest {

    @Test
    void testRoleConstructorAndGetters() {
        Role role = new Role("pk1", "sk1", "type1", "true", "Test description", "Admin");

        assertEquals("pk1", role.getPK());
        assertEquals("sk1", role.getSk());
        assertEquals("type1", role.getType());
        assertEquals("true", role.getActive());
        assertEquals("Test description", role.getDescription());
        assertEquals("Admin", role.getName());
    }

    @Test
    void testRoleSetters() {
        Role role = new Role();
        role.setPk("pk2");
        role.setSk("sk2");
        role.setType("type2");
        role.setActive("false");
        role.setDescription("Another description");
        role.setName("User");

        assertEquals("pk2", role.getPK());
        assertEquals("sk2", role.getSk());
        assertEquals("type2", role.getType());
        assertEquals("false", role.getActive());
        assertEquals("Another description", role.getDescription());
        assertEquals("User", role.getName());
    }

    @Test
    void testEqualsAndHashCode() {
        Role role1 = new Role("pk1", "sk1", "type1", "true", "Test description", "Admin");
        Role role2 = new Role("pk1", "sk1", "type1", "true", "Test description", "Admin");

        assertEquals(role1, role2);
        assertEquals(role1.hashCode(), role2.hashCode());
    }


    @Test
    void testToString() {
        Role role = new Role("pk1", "sk1", "type1", "true", "Test description", "Admin");
        String expected = "Role(pk=pk1, sk=sk1, type=type1, active=true, description=Test description, name=Admin)";
        assertEquals(expected, role.toString());
    }

    @Test
    void testNoArgsConstructor() {
        Role role = new Role();
        assertNull(role.getPK());
        assertNull(role.getSk());
        assertNull(role.getType());
        assertNull(role.getActive());
        assertNull(role.getDescription());
        assertNull(role.getName());
    }
}
package com.cognizant.lms.userservice.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUserTest {

    @Test
    public void testAuthUserConstructorWithAuthorities() {
        AuthUser authUser = new AuthUser(
                "testUser",
                "testPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertEquals("testUser", authUser.getUsername());
        assertEquals("testPassword", authUser.getPassword());
        assertTrue(authUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    public void testAuthUserConstructorWithAdditionalParameters() {
        AuthUser authUser = new AuthUser(
                "testUser",
                "testPassword",
                true,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertEquals("testUser", authUser.getUsername());
        assertEquals("testPassword", authUser.getPassword());
        assertTrue(authUser.isEnabled());
        assertTrue(authUser.isAccountNonExpired());
        assertTrue(authUser.isCredentialsNonExpired());
        assertTrue(authUser.isAccountNonLocked());
        assertTrue(authUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    public void testSettersAndGetters() {
        AuthUser authUser = new AuthUser(
                "testUser",
                "testPassword",
                Collections.emptyList()
        );

        authUser.setUserId("12345");
        authUser.setToken("testToken");
        authUser.setUserEmail("test@example.com");
        authUser.setUserRoles(List.of("ROLE_USER", "ROLE_ADMIN"));
        authUser.setFirstLogin(true);
        authUser.setViewOnlyAssignedCourses("true");

        assertEquals("12345", authUser.getUserId());
        assertEquals("testToken", authUser.getToken());
        assertEquals("test@example.com", authUser.getUserEmail());
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), authUser.getUserRoles());
        assertTrue(authUser.isFirstLogin());
        assertEquals("true", authUser.getViewOnlyAssignedCourses());
    }
}
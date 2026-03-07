package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.domain.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class UserContextTest {
    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCreatedBy_ShouldReturnUsername() {
        String expectedUsername = "currentUser";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);
        when(authUser.getUsername()).thenReturn(expectedUsername);

        String createdBy = UserContext.getCreatedBy();

        assertEquals(expectedUsername, createdBy);
    }

    @Test
    void getModifiedBy_ShouldReturnUsername() {
        String expectedUsername = "testUser";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);
        when(authUser.getUsername()).thenReturn(expectedUsername);

        String modifiedBy = UserContext.getModifiedBy();

        assertEquals(expectedUsername, modifiedBy);
    }
    @Test
    void getUserEmail_ShouldReturnUserEmail() {
        String expectedEmail = "user@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);
        when(authUser.getUserEmail()).thenReturn(expectedEmail);

        String userEmail = UserContext.getUserEmail();

        assertEquals(expectedEmail, userEmail);
    }
}

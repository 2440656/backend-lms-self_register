package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserActivityLogDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.domain.User;
import com.cognizant.lms.userservice.dto.UserActivityLogDto;
import com.cognizant.lms.userservice.exception.UserNotFoundException;
import com.cognizant.lms.userservice.utils.TokenUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserActivityLogServiceImplTest {

    @Mock
    private UserFilterSortDao userFilterSortDao;
    @Mock
    private UserActivityLogDao userActivityLogDao;

    @InjectMocks
    private UserActivityLogServiceImpl userActivityLogServiceImpl;

    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testSaveUserActivityLog_success() {
        AuthUser authUser = mock(AuthUser.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);
        when(authUser.getToken()).thenReturn("token123");
        when(authUser.getUserEmail()).thenReturn("test@example.com");

        try (MockedStatic<TokenUtil> tokenUtilMock = Mockito.mockStatic(TokenUtil.class)) {
            tokenUtilMock.when(() -> TokenUtil.extractIssuedAtTimeStamp("token123")).thenReturn("2024-01-01T00:00:00Z");

            User user = new User();
            user.setPk("user#1");
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmailId("test@example.com");
            when(userFilterSortDao.getUserByEmailId("test@example.com", Constants.ACTIVE_STATUS)).thenReturn(user);

            userActivityLogServiceImpl.saveUserActivityLog("Device", "127.0.0.1", "LOGIN");

            ArgumentCaptor<UserActivityLogDto> captor = ArgumentCaptor.forClass(UserActivityLogDto.class);
            verify(userActivityLogDao).saveUserActivityLog(captor.capture());
            UserActivityLogDto dto = captor.getValue();
            assert dto.getPk().equals("user#1");
            assert dto.getFirstName().equals("John");
            assert dto.getLastName().equals("Doe");
            assert dto.getEmailId().equals("test@example.com");
            assert dto.getDeviceDetails().equals("Device");
            assert dto.getIpAddress().equals("127.0.0.1");
            assert dto.getActivityType().equals("LOGIN");
            assert dto.getTimestamp().equals("2024-01-01T00:00:00Z");
            assert dto.getSk().startsWith("USER-ACTIVITY#");
        }
    }

    @Test
    void testSaveUserActivityLog_userNotFound_throwsException() {
        AuthUser authUser = mock(AuthUser.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);
        when(authUser.getToken()).thenReturn("token123");
        when(authUser.getUserEmail()).thenReturn("notfound@example.com");

        try (MockedStatic<TokenUtil> tokenUtilMock = Mockito.mockStatic(TokenUtil.class)) {
            tokenUtilMock.when(() -> TokenUtil.extractIssuedAtTimeStamp("token123")).thenReturn("2024-01-01T00:00:00Z");
            when(userFilterSortDao.getUserByEmailId("notfound@example.com", Constants.ACTIVE_STATUS))
                    .thenReturn(null);

            Assertions.assertThrows(UserNotFoundException.class, () ->
                            userActivityLogServiceImpl.saveUserActivityLog("Device", "127.0.0.1", "LOGIN")
            );
        }
    }

    @Test
    void testSaveUserActivityLog_nullAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);
        Assertions.assertThrows(NullPointerException.class, () ->
                userActivityLogServiceImpl.saveUserActivityLog("Device", "127.0.0.1", "LOGIN")
        );
    }

    @Test
    void testSaveUserActivityLog_nullPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);
        Assertions.assertThrows(NullPointerException.class, () ->
                userActivityLogServiceImpl.saveUserActivityLog("Device", "127.0.0.1", "LOGIN")
        );
    }

    @Test
        void testSaveUserActivityLog_nullToken() {
            AuthUser authUser = mock(AuthUser.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(authUser);
            when(authUser.getToken()).thenReturn(null);
            when(authUser.getUserEmail()).thenReturn("test@example.com");

            try (MockedStatic<TokenUtil> tokenUtilMock = Mockito.mockStatic(TokenUtil.class)) {
                tokenUtilMock.when(() -> TokenUtil.extractIssuedAtTimeStamp(anyString())).thenReturn(null);
                User user = new User();
                user.setPk("user#1");
                user.setFirstName("John");
                user.setLastName("Doe");
                user.setEmailId("test@example.com");
                when(userFilterSortDao.getUserByEmailId("test@example.com", Constants.ACTIVE_STATUS)).thenReturn(user);

                userActivityLogServiceImpl.saveUserActivityLog("Device", "127.0.0.1", "LOGIN");
                ArgumentCaptor<UserActivityLogDto> captor = ArgumentCaptor.forClass(UserActivityLogDto.class);
                verify(userActivityLogDao).saveUserActivityLog(captor.capture());
                Assertions.assertNull(captor.getValue().getTimestamp());
            }
        }
}
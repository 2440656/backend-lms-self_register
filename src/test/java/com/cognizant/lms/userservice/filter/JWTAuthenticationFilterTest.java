package com.cognizant.lms.userservice.filter;

import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dao.UserFilterSortDao;
import com.cognizant.lms.userservice.service.CognitoConfigService;
import com.cognizant.lms.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JWTAuthenticationFilterTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RoleDao roleDao;
    @Mock
    private UserFilterSortDao userFilterSortDao;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private CognitoConfigService cognitoConfigService;

    private JWTAuthenticationFilter filter;

    @Mock
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        filter = spy(new JWTAuthenticationFilter(
                authenticationManager,
                "certUrl",
                "clientId",
                "issuer",
                roleDao,
                userFilterSortDao,
                userService,
                cognitoConfigService
        ));
        lenient().when(response.getWriter()).thenReturn(printWriter);
    }

//    @Test
//    void doFilterInternal_publicUrl_allowsRequest() throws ServletException, IOException {
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/actuator/health"));
//        filter.doFilterInternal(request, response, filterChain);
//        verify(filterChain, times(1)).doFilter(request, response);
//    }
//
//    @Test
//    void doFilterInternal_noToken_returnsUnauthorized() throws ServletException, IOException {
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/protected"));
//        when(request.getHeader("Authorization")).thenReturn(null);
//
//        filter.doFilterInternal(request, response, filterChain);
//
//        verify(response, times(1)).setStatus(eq(401));
//    }
//
//    @Test
//    void sanitizeData_ssrfAttempt_throwsSecurityException() throws Exception {
//        String[] ssrfInputs = {"http://malicious", "https://malicious", "file://malicious"};
//        Method sanitizeData = JWTAuthenticationFilter.class.getDeclaredMethod("sanitizeData", String.class);
//        sanitizeData.setAccessible(true);
//        for (String input : ssrfInputs) {
//            assertThrows(SecurityException.class, () -> {
//                try {
//                    sanitizeData.invoke(filter, input);
//                } catch (Exception e) {
//                    // unwrap reflection exception
//                    if (e.getCause() instanceof SecurityException) throw (SecurityException) e.getCause();
//                    throw new RuntimeException(e);
//                }
//            });
//        }
//    }
//
//    // Helper methods for mocking (if you want to use them in future)
//    private Jws<Claims> createMockJwsWithClaims(String email, Map<String, String> customClaims, String alg, String tokenUse) {
//        // Not used in this fixed version, but you can use Mockito to mock Jws<Claims> if needed
//        return mock(Jws.class);
//    }
//
//    private Map<String, String> createCustomClaims() {
//        Map<String, String> map = new HashMap<>();
//        map.put("tenantCode", "tenant");
//        map.put("idpPreferences", "idp");
//        map.put("status", "ACTIVE");
//        map.put("role", "USER");
//        map.put("viewOnlyAssignedCourses", "false");
//        map.put("loginOption", "LMS_CREDENTIALS");
//        map.put("lastLoginTimestamp", "timestamp");
//        map.put("pk", "pk");
//        map.put("sk", "sk");
//        return map;
//    }
//
//    @Test
//    void doFilterInternal_tokenIsNull_returnsUnauthorized() throws Exception {
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/protected"));
//        when(request.getHeader("Authorization")).thenReturn(null);
//        filter.doFilterInternal(request, response, filterChain);
//        verify(response).setStatus(401);
//    }

}
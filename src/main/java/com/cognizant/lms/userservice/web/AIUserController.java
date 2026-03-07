package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.LoggedInUser;
import com.cognizant.lms.userservice.service.UserService;
import com.cognizant.lms.userservice.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/ai/users")
@Slf4j
public class AIUserController {
    @Autowired
    private UserService userService;

    @PostMapping("/current-user/register-login")
    @PreAuthorize("hasAnyRole('system-admin','super-admin','learner','mentor','content-author',"
            + "'catalog-admin')")
    public ResponseEntity<HttpResponse> registerLoggedInUser(HttpServletRequest request) {
        String deviceDetails = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();
        log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
                ProcessConstants.IN_PROGRESS) + "Fetching logged in user");
        HttpResponse response = new HttpResponse();
        LoggedInUser loggedInUser = userService.registerLoggedInUser(deviceDetails, ipAddress, "AI_PORTAL");

        response.setData(loggedInUser);
        response.setStatus(HttpStatus.OK.value());
        response.setError(null);
        log.info(LogUtil.getLogInfo(ProcessConstants.REGISTER_LOGGED_IN_USER,
                        ProcessConstants.COMPLETED) + "Fetched logged in user with userId {} and roles {}",
                loggedInUser.getUserId(), loggedInUser.getUserRoles());
        return ResponseEntity.ok().body(response);
    }


}

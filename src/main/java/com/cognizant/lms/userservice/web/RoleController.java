package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.domain.AuthUser;
import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.service.RoleService;
import com.cognizant.lms.userservice.utils.LogUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/roles")
@Slf4j
public class RoleController {

  @Autowired
  private RoleService roleService;

  @GetMapping()
  @PreAuthorize("hasAnyRole('system-admin','super-admin','operations')")
  public ResponseEntity<HttpResponse> getRoles() {
    log.info(LogUtil.getLogInfo(ProcessConstants.GET_ROLES,
        ProcessConstants.IN_PROGRESS) + "Fetching all roles");
    HttpResponse response = new HttpResponse();
    List<RoleDto> roles = roleService.getRoles();
    response.setData(roles);
    response.setStatus(HttpStatus.OK.value());
    response.setError(null);
    log.info(LogUtil.getLogInfo(ProcessConstants.GET_ROLES,
        ProcessConstants.COMPLETED) + "Fetching all roles {}", response.getData());
    return ResponseEntity.ok(response);
  }

}

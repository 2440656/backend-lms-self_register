package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.dao.RoleDao;
import com.cognizant.lms.userservice.dto.RoleDto;
import com.cognizant.lms.userservice.utils.LogUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {
  @Autowired
  private RoleDao roleDao;

  @Override
  public List<RoleDto> getRoles() {
    List<RoleDto> roleDtoList;
    try {
      roleDtoList = roleDao.getRoles();
    } catch (Exception e) {
      log.error(LogUtil.getLogError(ProcessConstants.GET_ROLES,
          HttpStatus.INTERNAL_SERVER_ERROR.toString(),
          ProcessConstants.FAILED) + "Failed to fetch all the roleDtoList {} ",
          e.getMessage(), e.getStackTrace());
      throw new RuntimeException(e.getMessage());
    }
    return roleDtoList;
  }
}

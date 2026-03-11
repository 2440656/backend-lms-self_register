package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.AspirationalRole;

import java.util.List;

public interface AspirationalRoleDao {
    
    List<AspirationalRole> getAllAspirationalRoles();
    
    List<AspirationalRole> getAspirationalRolesByType(String type);
}

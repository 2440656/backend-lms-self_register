package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.AspirationalRoleDao;
import com.cognizant.lms.userservice.dao.UserDao;
import com.cognizant.lms.userservice.domain.AspirationalRole;
import com.cognizant.lms.userservice.dto.AspirationalDataResponseDto;
import com.cognizant.lms.userservice.dto.AspirationalRoleDto;
import com.cognizant.lms.userservice.dto.UpdateProfileAspirationsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AspirationalRoleServiceImpl implements AspirationalRoleService {

    private final AspirationalRoleDao aspirationalRoleDao;
    private final UserDao userDao;

    public AspirationalRoleServiceImpl(AspirationalRoleDao aspirationalRoleDao, UserDao userDao) {
        this.aspirationalRoleDao = aspirationalRoleDao;
        this.userDao = userDao;
    }

    @Override
    public AspirationalDataResponseDto getAllAspirationalData() {
        try {
            log.info("Fetching all aspirational data");
            
            List<AspirationalRole> allRoles = aspirationalRoleDao.getAllAspirationalRoles();
            
            List<AspirationalRoleDto> userRoles = new ArrayList<>();
            List<AspirationalRoleDto> interests = new ArrayList<>();
            List<AspirationalRoleDto> roles = new ArrayList<>();
            
            for (AspirationalRole role : allRoles) {
                String pk = role.getPk();
                String sk = role.getSk();
                
                log.info("Processing item: PK='{}', SK='{}', userRoleName='{}', interestName='{}', roleName='{}'", 
                         pk, sk, role.getUserRoleName(), role.getInterestName(), role.getRoleName());
                
                if (pk == null || sk == null) {
                    log.warn("Skipping item with null PK or SK");
                    continue;
                }
                
                AspirationalRoleDto dto = mapToDto(role);
                
                // Check PK type
                if ("USERROLE".equals(pk)) {
                    userRoles.add(dto);
                    log.info("Added to userRoles: {}", dto.getName());
                } else if ("INTERESTS".equals(pk)) {
                    interests.add(dto);
                    log.info("Added to interests: {}", dto.getName());
                } else if ("ROLES".equals(pk)) {
                    roles.add(dto);
                    log.info("Added to roles: {}", dto.getName());
                } else {
                    log.warn("Unknown PK type: '{}' for SK: '{}'", pk, sk);
                }
            }
            
            log.info("Fetched {} user roles, {} interests, {} roles", 
                    userRoles.size(), interests.size(), roles.size());
            
            return AspirationalDataResponseDto.builder()
                    .userRoles(userRoles)
                    .interests(interests)
                    .roles(roles)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching aspirational data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch aspirational data", e);
        }
    }

    @Override
    public void updateUserAspirations(String pk, String sk, UpdateProfileAspirationsDto aspirationsDto) {
        try {
            log.info("Updating aspirations for user with pk: {}, sk: {}", pk, sk);
            
            String selectedUserRole = aspirationsDto.getSelectedUserRole();
            String selectedInterests = aspirationsDto.getSelectedInterests() != null 
                    ? String.join(",", aspirationsDto.getSelectedInterests()) 
                    : null;
            
            userDao.updateUserAspirations(pk, sk, selectedUserRole, selectedInterests);
            
            log.info("Successfully updated aspirations for user with pk: {}", pk);
            
        } catch (Exception e) {
            log.error("Error updating user aspirations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user aspirations", e);
        }
    }

    @Override
    public AspirationalDataResponseDto searchAspirationalData(AspirationalDataResponseDto allData, String query, String type) {
        try {
            log.info("Searching aspirational data with query: '{}', type: '{}'", query, type);
            
            if (query == null || query.trim().isEmpty()) {
                return allData;
            }
            
            String searchTerm = query.toLowerCase().trim();
            
            List<AspirationalRoleDto> filteredUserRoles = new ArrayList<>();
            List<AspirationalRoleDto> filteredInterests = new ArrayList<>();
            List<AspirationalRoleDto> filteredRoles = new ArrayList<>();
            
            // Filter based on type parameter
            if (type == null || "roles".equalsIgnoreCase(type) || "all".equalsIgnoreCase(type)) {
                filteredRoles = allData.getRoles().stream()
                        .filter(role -> role.getName().toLowerCase().contains(searchTerm))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (type == null || "interests".equalsIgnoreCase(type) || "all".equalsIgnoreCase(type)) {
                filteredInterests = allData.getInterests().stream()
                        .filter(interest -> interest.getName().toLowerCase().contains(searchTerm))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (type == null || "userRoles".equalsIgnoreCase(type) || "all".equalsIgnoreCase(type)) {
                filteredUserRoles = allData.getUserRoles().stream()
                        .filter(userRole -> userRole.getName().toLowerCase().contains(searchTerm))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            log.info("Search completed - Found {} user roles, {} interests, {} roles", 
                    filteredUserRoles.size(), filteredInterests.size(), filteredRoles.size());
            
            return AspirationalDataResponseDto.builder()
                    .userRoles(filteredUserRoles)
                    .interests(filteredInterests)
                    .roles(filteredRoles)
                    .build();
        } catch (Exception e) {
            log.error("Error searching aspirational data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search aspirational data", e);
        }
    }

    private AspirationalRoleDto mapToDto(AspirationalRole role) {
        String id = extractIdFromSk(role.getSk());
        String name = extractName(role);
        String type = role.getPk();
        
        return AspirationalRoleDto.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }

    private String extractIdFromSk(String sk) {
        if (sk != null && sk.contains("#")) {
            String[] parts = sk.split("#", 2);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return sk;
    }

    private String extractName(AspirationalRole role) {
        String pk = role.getPk();
        
        if ("USERROLE".equals(pk) && role.getUserRoleName() != null && !role.getUserRoleName().trim().isEmpty()) {
            return role.getUserRoleName().trim();
        } else if ("INTERESTS".equals(pk) && role.getInterestName() != null && !role.getInterestName().trim().isEmpty()) {
            return role.getInterestName().trim();
        } else if ("ROLES".equals(pk) && role.getRoleName() != null && !role.getRoleName().trim().isEmpty()) {
            return role.getRoleName().trim();
        }
        
        String id = extractIdFromSk(role.getSk());
        return id != null ? id.replace("_", " ") : "Unknown";
    }
}

package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.AspirationalDataResponseDto;
import com.cognizant.lms.userservice.dto.UpdateProfileAspirationsDto;

public interface AspirationalRoleService {
    
    AspirationalDataResponseDto getAllAspirationalData();
    
    AspirationalDataResponseDto searchAspirationalData(AspirationalDataResponseDto allData, String query, String type);

    void updateUserAspirations(String pk, String sk, UpdateProfileAspirationsDto aspirationsDto);
}

package com.cognizant.lms.userservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerManagementResponse {
    private List<BannerManagementDto> bannerManagementList;
    private int totalActiveRecords;

}

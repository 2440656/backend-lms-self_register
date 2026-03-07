package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularLinkDto {
    private String linkId;
    private String title;
    private String url;
    private String description;
    private Integer index;
    private String iconKey;
    private String iconFileName;
}

package com.cognizant.lms.userservice.dto;

import com.cognizant.lms.userservice.domain.SkillLookups;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SkillsCSVProcessResponse {

    private List<SkillLookups> validSkills;
    private List<String> errors;
    private int successCount;
    private int failureCount;
    private int totalCount;

}

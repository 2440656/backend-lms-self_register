package com.cognizant.lms.userservice.dto;

import com.cognizant.lms.userservice.domain.SkillLookups;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillLookupResponse {
  private List<SkillLookups> skills;
  private String lastEvaluatedKey;
  private int status;
  private String error;
}

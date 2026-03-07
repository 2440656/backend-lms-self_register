package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.SkillLookups;
import com.cognizant.lms.userservice.dto.SkillCategoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;

import java.util.List;

public interface SkillLookupsDao {

  List<SkillCategoryResponse> getSkillCategory(String skillName);

  SkillLookupResponse getSkillsAndLookupsByNameOrCode(String type, String search);

  void uploadSkills(List<SkillLookups> skills);
}

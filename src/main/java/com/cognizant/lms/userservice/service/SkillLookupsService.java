package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.FileUploadResponse;
import com.cognizant.lms.userservice.dto.SkillCategoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SkillLookupsService {

  SkillLookupResponse getSkillsLookupsByTypeNameOrCode(String type, String search);

  List<SkillCategoryResponse> getSkillCategory(String skillNames);

  /**
   * method to upload skills from file
   *
   * @param file                    the file to be uploaded
   * @param action                  the action to be performed
   * @return FileUploadResponse     the response of the file upload
   * @throws Exception              if any error occurs
   */
  FileUploadResponse uploadSkills(MultipartFile file, String action) throws Exception;

    /**
     * Method to download error log file for skills upload
     * @param filename             the name of the file
     * @param fileType             the type of the file
     * @return Resource            the resource of the file
     * @throws Exception           if any error occurs
     */
  Resource getDownloadErrorLogFileForSkills(String filename, String fileType) throws Exception;

}

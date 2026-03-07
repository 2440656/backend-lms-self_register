package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.OperationsHistoryDao;
import com.cognizant.lms.userservice.dao.SkillLookupsDao;
import com.cognizant.lms.userservice.domain.OperationsHistory;
import com.cognizant.lms.userservice.dto.FileUploadResponse;
import com.cognizant.lms.userservice.dto.SkillCategoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;
import com.cognizant.lms.userservice.dto.SkillsCSVProcessResponse;


import com.cognizant.lms.userservice.utils.CustomMultipartFile;
import com.cognizant.lms.userservice.utils.S3Util;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import com.cognizant.lms.userservice.utils.FileUtil;
import com.cognizant.lms.userservice.utils.SkillCSVProcessor;
import com.cognizant.lms.userservice.utils.Base64Util;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cognizant.lms.userservice.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class SkillLookupsServiceImpl implements SkillLookupsService {

    private final SkillLookupsDao skillLookupsDao;
    private final OperationsHistoryDao operationsHistoryDao;
    private final SkillCSVProcessor skillCSVProcessor;
    private final String bucketName;
    private final String localStoragePath;
    private final String applicationEnv;
    private final S3Util s3Utils;
    private final FileUtil fileUtil;

    @Autowired
    public SkillLookupsServiceImpl(SkillLookupsDao skillLookupsDao,
                                   OperationsHistoryDao operationsHistoryDao,
                                   SkillCSVProcessor skillCSVProcessor,
                                   @Value("${AWS_S3_BUCKET_NAME}") String bucketName,
                                   @Value("${LOCAL_STORAGE_PATH}") String localStoragePath,
                                   @Value("${APP_ENV}") String applicationEnv,
                                   S3Util s3Utils,
                                   FileUtil fileUtil) {
        this.skillLookupsDao = skillLookupsDao;
        this.operationsHistoryDao = operationsHistoryDao;
        this.skillCSVProcessor = skillCSVProcessor;
        this.bucketName = bucketName;
        this.localStoragePath = localStoragePath;
        this.applicationEnv = applicationEnv;
        this.s3Utils = s3Utils;
        this.fileUtil = fileUtil;
    }

    @Override
    public SkillLookupResponse getSkillsLookupsByTypeNameOrCode(String type, String search) {
        if (search != null && !search.isEmpty()) {
            search = search.toLowerCase().trim();
        }

        return skillLookupsDao.getSkillsAndLookupsByNameOrCode(type, search);
    }


    @Override
    public List<SkillCategoryResponse> getSkillCategory(String skillName) {
        return skillLookupsDao.getSkillCategory(skillName);
    }

    @Override
    public FileUploadResponse uploadSkills(MultipartFile file, String action) throws Exception {
        FileUploadResponse response = new FileUploadResponse();
        String fileResponse = uploadBulkSkillsFile(file);
        if (fileResponse.contains(Constants.FILE_SAVED_MESSAGE)) {
            SkillsCSVProcessResponse skillsCSVProcessResponse = skillCSVProcessor.processFile(file);
            String errorLogFileName = generateErrorLogFileName(file);
            String createdBy = UserContext.getCreatedBy();
            if (skillsCSVProcessResponse.getSuccessCount() != 0 && isProcessingSuccessful(skillsCSVProcessResponse)) {
                handleSkillsUploading(skillsCSVProcessResponse, response, file, createdBy, "Success");
            } else if (skillsCSVProcessResponse.getSuccessCount() != 0 && skillsCSVProcessResponse.getFailureCount() != 0
                    && isPartialProcessing(skillsCSVProcessResponse)) {
                handleSkillsUploadingAndErrLogging(file, action, skillsCSVProcessResponse, errorLogFileName, response, createdBy, "Error");
            } else {
                handleErrLogging(file, action, skillsCSVProcessResponse, errorLogFileName, response, "Failed");
            }
        }
        return response;
    }


    private void handleSkillsUploading(SkillsCSVProcessResponse skillsCSVProcessResponse,
                                       FileUploadResponse response,
                                       MultipartFile file, String createdBy, String uploadStatus) {
        log.info("Uploading {} skills", skillsCSVProcessResponse.getValidSkills().size());
        skillLookupsDao.uploadSkills(skillsCSVProcessResponse.getValidSkills());
        log.info("Uploaded successfully {} skills data to the database.",
                skillsCSVProcessResponse.getValidSkills().size());
        // Generate and persist a log file for successful uploads
        String logFileName = generateErrorLogFileName(file);
        String[][] logData = new String[skillsCSVProcessResponse.getValidSkills().size()][Constants.uploadSkillsLogHeaders.size()];
        try {
            MultipartFile logFile = createErrorLogFile(
                    Constants.uploadSkillsLogHeaders.toArray(new String[0]),
                    logData,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_DATE_FORMAT)),
                    file.getOriginalFilename(),
                    logFileName,
                    0,
                    skillsCSVProcessResponse.getSuccessCount(),
                    skillsCSVProcessResponse.getTotalCount(),
                    Constants.ACTION_UPLOAD_SKILLS
            );
            boolean isLogSaved = applicationEnv.equalsIgnoreCase(Constants.appEnv)
                    ? fileUtil.saveFileToLocal(logFile, localStoragePath, Constants.LOCAL_DISK_PREFIX_SKILLS_ERRORLOG)
                    : s3Utils.saveFileToS3(logFile, bucketName, TenantUtil.getTenantCode() + Constants.SKILLS_ERROR_LOG_PREFIX + logFileName);
            if (isLogSaved) {
                persistSkillLogFileData(Constants.ACTION_UPLOAD_SKILLS, logFileName, uploadStatus);
            } else {
                log.error("Failed to save log file: {}", logFileName);
                return;
            }
        } catch (IOException e) {
            log.error("Error creating or saving log file: {}", e.getMessage());
            return;
        }
        response.setSuccessMessage("File " + file.getOriginalFilename()
                + " processed successfully with " + skillsCSVProcessResponse.getTotalCount()
                + " records verified and 0 errors.");
    }

    private void handleSkillsUploadingAndErrLogging(MultipartFile file, String action,
                                                    SkillsCSVProcessResponse skillsCSVProcessResponse,
                                                    String errorLogFileName,
                                                    FileUploadResponse response,
                                                    String createdBy, String uploadStatus) throws Exception {
        if (skillsCSVProcessResponse.getSuccessCount() != 0) {
            log.info("Handling error while uploading {} skills to the database", skillsCSVProcessResponse.getValidSkills().size());
            skillLookupsDao.uploadSkills(skillsCSVProcessResponse.getValidSkills());
            log.info("Uploaded {} skills data to the database.", skillsCSVProcessResponse.getValidSkills().size());
        }
        log.info("Error list {}", skillsCSVProcessResponse.getErrors().stream().distinct().toList());
        boolean isErrorLogCreated = saveErrorLogFileForUploadSkills(skillsCSVProcessResponse.getErrors().stream().distinct().toList(),
                errorLogFileName, file.getOriginalFilename(), skillsCSVProcessResponse.getFailureCount(),
                skillsCSVProcessResponse.getSuccessCount(), skillsCSVProcessResponse.getTotalCount(), action, uploadStatus);
        if (!isErrorLogCreated) {
            throw new IOException("Failed to save file : " + errorLogFileName);
        }
        response.setErrorLogFileName(errorLogFileName);
        throw new Exception("File " + file.getOriginalFilename() + " processed partially with "
                + skillsCSVProcessResponse.getSuccessCount() + " records verified and "
                + skillsCSVProcessResponse.getFailureCount() + " errors.");
    }

    private void handleErrLogging(MultipartFile file, String action,
                                  SkillsCSVProcessResponse skillsCSVProcessResponse,
                                  String errorLogFileName,
                                  FileUploadResponse response, String uploadStatus) throws Exception {
        log.info("Error list {}", skillsCSVProcessResponse.getErrors().stream().distinct().toList());
        boolean isErrorLogCreated = saveErrorLogFileForUploadSkills(skillsCSVProcessResponse.getErrors().stream().distinct().toList(),
                errorLogFileName, file.getOriginalFilename(), skillsCSVProcessResponse.getFailureCount(),
                skillsCSVProcessResponse.getSuccessCount(), skillsCSVProcessResponse.getTotalCount(), action, uploadStatus);
        if (!isErrorLogCreated) {
            throw new IOException("Failed to save file : " + errorLogFileName);
        }
        response.setErrorLogFileName(errorLogFileName);
        throw new Exception("File " + file.getOriginalFilename() + " failed with "
                + skillsCSVProcessResponse.getFailureCount() + " errors.");
    }

    private boolean saveErrorLogFileForUploadSkills(List<String> errors, String logFileName,
                                                    String originalFileName,
                                                    int failureCount, int successCount, int totalCount,
                                                    String action, String uploadStatus) throws IOException {
        String[][] errorData = new String[errors.size()][Constants.uploadSkillsLogHeaders.size()];
        String fileUploadTime = null;
        for (int i = 0; i < errors.size(); i++) {
            String[] err = errors.get(i).split("--");
            fileUploadTime = err[0];
            errorData[i][0] = err[0];
            errorData[i][1] = err.length > 2 ? err[1] : " ";
            errorData[i][2] = err.length > 2 ? err[2] : " ";
            errorData[i][3] = err.length > 2 ? err[3] : " ";
            errorData[i][4] = err.length > 2 ? err[4] : err[1];
        }
        MultipartFile errorLogFile = createErrorLogFile(Constants.uploadSkillsLogHeaders.toArray(new String[0]), errorData,
                fileUploadTime, originalFileName, logFileName, failureCount, successCount, totalCount, action);
        boolean isSaved = applicationEnv.equalsIgnoreCase(Constants.appEnv)
                ? fileUtil.saveFileToLocal(errorLogFile, localStoragePath, Constants.LOCAL_DISK_PREFIX_SKILLS_ERRORLOG)
                : s3Utils.saveFileToS3(errorLogFile, bucketName, TenantUtil.getTenantCode() + Constants.SKILLS_ERROR_LOG_PREFIX + logFileName);

        if (isSaved) {
            persistSkillLogFileData(action, logFileName, uploadStatus);
        }
        return isSaved;
    }

    private MultipartFile createErrorLogFile(String[] logsHeaders,
                                             String[][] errorData, String fileUploadTime,
                                             String originalFileName,
                                             String logFileName,
                                             int failureCount, int successCount, int totalCount,
                                             String action) throws IOException {
        Column[] columns = new Column[logsHeaders.length];
        for (int i = 0; i < logsHeaders.length; i++) {
            columns[i] = new Column().header(logsHeaders[i]).headerAlign(HorizontalAlign.LEFT)
                    .dataAlign(HorizontalAlign.LEFT);
        }
        String table = AsciiTable.getTable(AsciiTable.NO_BORDERS, columns, errorData);
        String multiLineError =
                String.format(Constants.MULTILINE_ERROR_LOG_FORMAT, originalFileName,
                        fileUploadTime, totalCount, successCount, failureCount, action);
        List<String> errorLogs = new ArrayList<>();
        errorLogs.add(multiLineError);
        errorLogs.add(table);
        return new CustomMultipartFile(errorLogs, logFileName);
    }

    private String uploadBulkSkillsFile(MultipartFile file) throws Exception {
        log.info("Uploading file {} for bulk skills creation", file.getOriginalFilename());
        fileUtil.validateFile(file);
        boolean isFileSaved = false;
        String storageLocationMsg = "";
        if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
            isFileSaved = fileUtil.saveFileToLocal(file, localStoragePath, Constants.LOCAL_DISK_PREFIX_SKILLS_MASTERDATA);
            storageLocationMsg = Constants.storedInLocalMsg;
        } else {
            isFileSaved = s3Utils.saveFileToS3(file, bucketName,
                    TenantUtil.getTenantCode() + Constants.S3_SKILLS_MASTERDATA_PREFIX + file.getOriginalFilename());
            storageLocationMsg = Constants.storedInS3Msg;
        }
        if (!isFileSaved) {
            throw new FileStorageException("Failed to save file " + file.getOriginalFilename());
        }
        return storageLocationMsg;
    }

    private String generateErrorLogFileName(MultipartFile file) {
        return file.getOriginalFilename()
                + Constants.LOG_FILE_SUFFIX
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.LOG_FILE_TIMESTAMP_FORMAT))
                + Constants.LOG_FILE_EXTENSION;
    }

    private boolean isProcessingSuccessful(SkillsCSVProcessResponse skillsCSVProcessResponse) {
        return skillsCSVProcessResponse.getFailureCount() == 0
                && skillsCSVProcessResponse.getSuccessCount() == skillsCSVProcessResponse.getTotalCount();
    }

    private boolean isPartialProcessing(SkillsCSVProcessResponse skillsCSVProcessResponse) {
        return skillsCSVProcessResponse.getSuccessCount() + skillsCSVProcessResponse.getFailureCount()
                == skillsCSVProcessResponse.getTotalCount();
    }

    private void persistSkillLogFileData(String action, String fileName, String uploadStatus) {
        try {
            OperationsHistory logFileData = new OperationsHistory();
            logFileData.setPk(
                    TenantUtil.getTenantCode() + Constants.HASH + Constants.AREA_SKILL_MANAGEMENT);
            logFileData.setSk(ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern(Constants.SKILL_LOG_CREATED_ON_TIMESTAMP))
                    + Constants.HASH + action);
            logFileData.setFileName(fileName);
            logFileData.setCreatedOn(ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern(Constants.SKILL_LOG_CREATED_ON_TIMESTAMP)));
            logFileData.setUploadedBy(UserContext.getCreatedBy());
            logFileData.setEmail(UserContext.getUserEmail());
            logFileData.setTenantCode(TenantUtil.getTenantCode());
            logFileData.setOperation(action);
            logFileData.setArea(Constants.AREA_SKILL_MANAGEMENT);
            logFileData.setUploadStatus(uploadStatus);
            log.info("Generating skill log file data for id: {}", logFileData.getPk());
            operationsHistoryDao.saveLogFileData(logFileData);
        } catch (Exception e) {
            log.error("Failed to persist skill log file data: {}", e.getMessage());
            throw new RuntimeException("Failed to persist skill log file data: " + e.getMessage());
        }
    }

    @Override
    public Resource getDownloadErrorLogFileForSkills(String filename, String fileType) throws Exception {
        Path filePath;
        if (applicationEnv.equalsIgnoreCase(Constants.appEnv)) {
            if (Constants.FILE_TYPE_TXT.equalsIgnoreCase(fileType)) {
                filePath = Paths.get(localStoragePath + Constants.LOCAL_DISK_PREFIX_SKILLS_ERRORLOG)
                        .resolve(filename).normalize();
            } else if (Constants.FILE_TYPE_CSV.equalsIgnoreCase(fileType)) {
                filePath = Paths.get(localStoragePath + Constants.LOCAL_DISK_PREFIX_SKILLS_MASTERDATA)
                        .resolve(filename).normalize();
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }
            if (!Files.exists(filePath)) {
                log.error("File not found: {}", filename);
                throw new FileNotFoundException("File not found: " + filename);
            }
            return new InputStreamResource(Files.newInputStream(filePath));
        } else {
            try {
                return s3Utils.downloadFileFromS3(filename, fileType, bucketName, true);
            } catch (Exception e) {
                log.error("Error downloading file from S3: {}", e.getMessage());
                throw new Exception("Error downloading file from S3");
            }
        }
    }
}

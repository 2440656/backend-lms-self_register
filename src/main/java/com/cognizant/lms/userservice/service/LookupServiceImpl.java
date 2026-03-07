package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.S3config;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.constants.ProcessConstants;
import com.cognizant.lms.userservice.dao.LookupDao;
import com.cognizant.lms.userservice.dto.AiVoicePreviewLookupDto;
import com.cognizant.lms.userservice.dto.LookupDto;
import com.cognizant.lms.userservice.utils.LogUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class LookupServiceImpl implements LookupService {

    private LookupDao lookupDao;
    private final String aiVoicePreviewBucketName;
    private final S3Presigner s3Presigner;
    private final String appEnv;

    @Autowired
    public LookupServiceImpl(LookupDao lookupDao, S3config s3config,
                             @Value("${AWS_S3_AI_VOICE_PREVIEW_BUCKET_NAME}") String aiVoicePreviewBucketName,
                             @Value("${APP_ENV}") String appEnv) {
        this.lookupDao = lookupDao;
        this.s3Presigner = s3config.s3Presigner();
        this.aiVoicePreviewBucketName = aiVoicePreviewBucketName;
        this.appEnv = appEnv;
    }

    @Autowired
    public  void setLookupDao(LookupDao lookupDao){
        this.lookupDao = lookupDao;
    }

    @Override
    public List<LookupDto> getLookupsList(String type, String skSuffix) {
        List<LookupDto> lookupDtoList;
        try{
            lookupDtoList = lookupDao.getLookupData(type,skSuffix);
        } catch (Exception e){
            log.error(LogUtil.getLogError("GET_LOOKUP",
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    ProcessConstants.FAILED) + "Failed to fetch all the lookupDtoList {} ",
                    e.getMessage(), e.getStackTrace());
            throw new RuntimeException(e.getMessage());
        }
        return lookupDtoList;
    }

    @Override
    public List<LookupDto> getServiceLineLookupsList(String type, String skSuffix) {
        List<LookupDto> lookupDtoList;
        try{
            lookupDtoList = lookupDao.getServiceLineLookupData(type,skSuffix);
        } catch (Exception e){
            log.error(LogUtil.getLogError("GET_LOOKUP",
                            HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                            ProcessConstants.FAILED) + "Failed to fetch all the lookupDtoList {} ",
                    e.getMessage(), e.getStackTrace());
            throw new RuntimeException(e.getMessage());
        }
        return lookupDtoList;
    }

    @Override
    public List<AiVoicePreviewLookupDto> generateAiVoicePreviewUrls() {
        List<AiVoicePreviewLookupDto> aiVoicePreviewLookupDtoList = lookupDao.getAiVoicePreviewData();
        if (!appEnv.equals(Constants.LOCAL)) {
            Duration expiry = Duration.ofHours(12);
            for (AiVoicePreviewLookupDto dto : aiVoicePreviewLookupDtoList) {
                String s3Path = dto.getPath() + "/";
                String fileKey = s3Path + dto.getFileName();
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(aiVoicePreviewBucketName)
                    .key(fileKey)
                    .build();
                PresignedGetObjectRequest presignedRequest =
                    s3Presigner.presignGetObject(r -> r
                        .getObjectRequest(getObjectRequest)
                        .signatureDuration(expiry));
                dto.setPreSignedUrl(presignedRequest.url().toExternalForm());
            }
        }
        return aiVoicePreviewLookupDtoList;
    }
}

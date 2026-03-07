package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dto.TenantDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

public class S3UtilTest {
    @Mock
    private S3Client s3ThumbnailClient;

    @Mock
    private MultipartFile file;

    private S3Util s3Util;

    @BeforeEach
    void setUp() {
        TenantUtil.setTenantDetails(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));
        MockitoAnnotations.openMocks(this);
        s3Util = new S3Util(s3ThumbnailClient);
    }

    @Test
    void saveFileToS3_ShouldReturnTrue_WhenFileIsSaved() throws IOException {
        String bucketName = "test-bucket";
        String key = "test-key";
        String originalFileName = "test-file.txt";

        when(file.getOriginalFilename()).thenReturn(originalFileName);
        when(file.getBytes()).thenReturn(new byte[0]);

        doReturn(null).when(s3ThumbnailClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        doReturn(mock(InputStream.class)).when(s3ThumbnailClient).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));    doReturn(mock(InputStream.class)).when(s3ThumbnailClient).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
        boolean result = s3Util.saveFileToS3(file, bucketName, key);

        assertTrue(result);
        verify(s3ThumbnailClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void saveFileToS3_ShouldReturnFalse_WhenS3ExceptionOccurs() throws IOException {
        String bucketName = "test-bucket";
        String key = "test-key";

        when(file.getBytes()).thenReturn(new byte[0]);
        doThrow(S3Exception.class).when(s3ThumbnailClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        boolean result = s3Util.saveFileToS3(file, bucketName, key);

        assertFalse(result);
    }

    @Test
    void confirmFileUploadToS3_ShouldReturnTrue_WhenFileIsPresent() {
        String bucketName = "test-bucket";
        String key = "test-key";
        String originalFileName = "test-file.txt";

        doReturn(mock(InputStream.class)).when(s3ThumbnailClient).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
        boolean result = s3Util.confirmFileUploadToS3(originalFileName, bucketName, key);

        assertTrue(result);
    }

//    @Test
//    void confirmFileUploadToS3_ShouldReturnFalse_WhenFileIsNotPresent() {
//        String bucketName = "test-bucket";
//        String key = "test-key";
//        String originalFileName = "test-file.txt";
//
//        doThrow(S3Exception.class).when(s3Client).getObject(any(GetObjectRequest.class));
//
//        boolean result = s3Util.confirmFileUploadToS3(originalFileName, bucketName, key);
//
//        assertFalse(result);
//    }
    @Test
    void downloadFileFromS3_ShouldReturnResource_WhenFileIsDownloaded() throws Exception {
        String filename = "test-file.txt";
        String fileType = Constants.FILE_TYPE_TXT;
        String bucketName = "test-bucket";

        when(s3ThumbnailClient.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class))).thenReturn(mock(InputStream.class));

        Resource resource = s3Util.downloadFileFromS3(filename, fileType, bucketName, false);

        assertNotNull(resource);
    }

    @Test
    void downloadFileFromS3_s3Exception_throwsIOException() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder().errorMessage("S3 error").build();
        doThrow(S3Exception.builder().awsErrorDetails(errorDetails).build())
                .when(s3ThumbnailClient).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));

        assertThrows(IOException.class, () -> s3Util.downloadFileFromS3("test.txt", "txt", "bucketName", false));
    }

    @Test
    void downloadFileFromS3_skillFile_txtType_returnsResource() throws Exception {
        String filename = "skill-error.txt";
        String fileType = Constants.FILE_TYPE_TXT;
        String bucketName = "test-bucket";

        when(s3ThumbnailClient.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenReturn(mock(InputStream.class));

        Resource resource = s3Util.downloadFileFromS3(filename, fileType, bucketName, true);

        assertNotNull(resource);
    }

    @Test
    void downloadFileFromS3_skillFile_csvType_returnsResource() throws Exception {
        String filename = "skills-master.csv";
        String fileType = Constants.FILE_TYPE_CSV;
        String bucketName = "test-bucket";

        when(s3ThumbnailClient.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenReturn(mock(InputStream.class));

        Resource resource = s3Util.downloadFileFromS3(filename, fileType, bucketName, true);

        assertNotNull(resource);
    }

    @Test
    void downloadFileFromS3_skillFile_unsupportedType_throwsIllegalArgumentException() {
        String filename = "skills.unknown";
        String fileType = "unknown";
        String bucketName = "test-bucket";

        assertThrows(IllegalArgumentException.class, () ->
                s3Util.downloadFileFromS3(filename, fileType, bucketName, true)
        );
    }
}

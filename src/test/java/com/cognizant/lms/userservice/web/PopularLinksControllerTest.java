package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.*;
import com.cognizant.lms.userservice.exception.PopularLinkLimitException;
import com.cognizant.lms.userservice.service.PopularLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class PopularLinksControllerTest {

    private PopularLinkService popularLinkService;
    private PopularLinksController controller;

    @BeforeEach
    void setUp() {
        popularLinkService = Mockito.mock(PopularLinkService.class);
        controller = new PopularLinksController(popularLinkService);
    }

    @Test
    void savePopularLink_multipart_success() {
        PopularLinkRequestDto dto = new PopularLinkRequestDto();
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        PopularLinkDto result = new PopularLinkDto();
        MockMultipartFile file = new MockMultipartFile("file", "icon.png", "image/png", new byte[]{1, 2, 3});
        when(popularLinkService.savePopularLink(any(PopularLinkRequestDto.class), any(MultipartFile.class))).thenReturn(result);
        String json = "{\"title\":\"Test Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";
        ResponseEntity<HttpResponse> response = controller.savePopularLink(json, file);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void savePopularLink_multipart_invalidJson() {
        ResponseEntity<HttpResponse> response = controller.savePopularLink("not-json", null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request body, title,URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void savePopularLink_multipart_validationError() {
        String json = "{\"title\":\"\",\"url\":\"\",\"description\":\"\"}";
        ResponseEntity<HttpResponse> response = controller.savePopularLink(json, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().getError(), "Error message should not be null");
        assertEquals("Request body, title and URL must not be empty", response.getBody().getError());
    }

    @Test
    void savePopularLink_multipart_limitException() {
        String json = "{\"title\":\"Test\",\"url\":\"http://test.com\",\"description\":\"desc\"}";
        when(popularLinkService.savePopularLink(any(PopularLinkRequestDto.class), any()))
            .thenThrow(new PopularLinkLimitException("limit reached"));
        ResponseEntity<HttpResponse> response = controller.savePopularLink(json, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("limit reached", response.getBody().getError());
    }

    @Test
    void savePopularLink_multipart_genericException() {
        String json = "{\"title\":\"Test\",\"url\":\"http://test.com\",\"description\":\"desc\"}";
        when(popularLinkService.savePopularLink(any(PopularLinkRequestDto.class), any()))
            .thenThrow(new RuntimeException("fail"));
        ResponseEntity<HttpResponse> response = controller.savePopularLink(json, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().getError(), "Error message should not be null");
        assertTrue(response.getBody().getError().startsWith("Failed to save popular link"));
    }

    @Test
    void updatePopularLink_multipart_success() {
        PopularLinkRequestDto dto = new PopularLinkRequestDto();
        dto.setLinkId("id");
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        PopularLinkDto result = new PopularLinkDto();
        MockMultipartFile file = new MockMultipartFile("file", "icon.png", "image/png", new byte[]{1, 2, 3});
        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            any(MultipartFile.class),
            eq(false),
            eq(false)
        )).thenReturn(result);

        String json = "{\"linkId\":\"id\",\"title\":\"Test Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";
        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, file, "false", "false");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void updatePopularLink_multipart_invalidJson() {
        ResponseEntity<HttpResponse> response = controller.updatePopularLink("not-json", null, "false", "false");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid linkRequest JSON", response.getBody().getError());
    }

    @Test
    void updatePopularLink_multipart_missingLinkId() {
        String json = "{\"title\":\"Test Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";
        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, null, "false", "false");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request body, title,URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void updatePopularLink_multipart_exception() {
        String json = "{\"linkId\":\"id\",\"title\":\"Test Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";
        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            any(),
            anyBoolean(),
            anyBoolean()
        )).thenThrow(new RuntimeException("fail"));
        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, null, "false", "false");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals("Failed to update popular link", response.getBody().getError());
    }

    @Test
    void updatePopularLink_withNewIcon_success() {
        PopularLinkDto result = new PopularLinkDto();
        result.setLinkId("id1");
        result.setTitle("Updated Title");
        result.setIconFileName("new-icon.png");

        MockMultipartFile file = new MockMultipartFile(
            "file", "new-icon.png",
            "image/png",
            "test image content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            any(MultipartFile.class),
            eq(false),
            eq(false)
        )).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, file, "false", "false");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());

        verify(popularLinkService).updatePopularLink(
            any(PopularLinkRequestDto.class),
            eq(file),
            eq(false),
            eq(false)
        );
    }

    @Test
    void updatePopularLink_keepExistingIcon_success() {
        PopularLinkDto result = new PopularLinkDto();
        result.setLinkId("id1");
        result.setTitle("Updated Title");
        result.setIconFileName("existing-icon.png");

        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            isNull(),
            eq(true),
            eq(false)
        )).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, null, "true", "false");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertEquals(result, body.getData());
        assertNull(body.getError());
    }

    @Test
    void updatePopularLink_removeExistingIcon_success() {
        PopularLinkDto result = new PopularLinkDto();
        result.setLinkId("id1");
        result.setTitle("Updated Title");
        result.setIconFileName(null);
        result.setIconKey(null);

        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            isNull(),
            eq(false),
            eq(true)
        )).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, null, "false", "true");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertEquals(result, body.getData());
        assertNull(body.getError());
    }

    @Test
    void updatePopularLink_replaceExistingIcon_success() {
        PopularLinkDto result = new PopularLinkDto();
        result.setLinkId("id1");
        result.setTitle("Updated Title");
        result.setIconFileName("replacement-icon.png");

        MockMultipartFile file = new MockMultipartFile(
            "file", "replacement-icon.png",
            "image/png",
            "new image content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            any(MultipartFile.class),
            eq(false),
            eq(false)
        )).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, file, "false", "false");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertEquals(result, body.getData());
        assertNull(body.getError());

        verify(popularLinkService).updatePopularLink(
            any(PopularLinkRequestDto.class),
            eq(file),
            eq(false),
            eq(false)
        );
    }

    @Test
    void updatePopularLink_invalidFlagCombination_success() {
        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        PopularLinkDto result = new PopularLinkDto();
        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            isNull(),
            eq(true),
            eq(true)
        )).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, null, "true", "true");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        verify(popularLinkService).updatePopularLink(
            any(PopularLinkRequestDto.class),
            isNull(),
            eq(true),
            eq(true)
        );
    }

    @Test
    void updatePopularLink_withLargeFile_returnsInternalError() {
        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        MockMultipartFile file = new MockMultipartFile(
            "file", "large-icon.png",
            "image/png",
            new byte[2 * 1024 * 1024]
        );

        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            any(MultipartFile.class),
            anyBoolean(),
            anyBoolean()
        )).thenThrow(new IllegalArgumentException("File size exceeds limit"));

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, file, "false", "false");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertNotNull(body.getError(), "Error message should not be null");
        assertTrue(body.getError().contains("Failed to update popular link"));
    }

    @Test
    void updatePopularLink_withInvalidImageType_returnsInternalError() {
        String json = "{\"linkId\":\"id1\",\"title\":\"Updated Title\",\"url\":\"http://test.com\",\"description\":\"desc\"}";

        MockMultipartFile file = new MockMultipartFile(
            "file", "document.pdf",
            "application/pdf",
            "test content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(popularLinkService.updatePopularLink(
            any(PopularLinkRequestDto.class),
            any(MultipartFile.class),
            anyBoolean(),
            anyBoolean()
        )).thenThrow(new IllegalArgumentException("Invalid file type"));

        ResponseEntity<HttpResponse> response = controller.updatePopularLink(json, file, "false", "false");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        HttpResponse body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertNotNull(body.getError(), "Error message should not be null");
        assertTrue(body.getError().contains("Failed to update popular link"));
    }
}

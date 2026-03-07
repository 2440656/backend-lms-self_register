package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.*;
import com.cognizant.lms.userservice.exception.QuickLinkLimitException;
import com.cognizant.lms.userservice.service.QuickLinkService;
import com.cognizant.lms.userservice.web.QuickLinksController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class QuickLinksControllerTest {

    private QuickLinkService quickLinkService;
    private QuickLinksController controller;

    @BeforeEach
    void setUp() {
        quickLinkService = Mockito.mock(QuickLinkService.class);
        controller = new QuickLinksController(quickLinkService);
    }

    @Test
    void saveQuickLink_success() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        QuickLinkDto result = new QuickLinkDto();
        result.setLinkId("id1");
        when(quickLinkService.saveQuickLink(any(QuickLinkRequestDto.class))).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.saveQuickLink(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void saveQuickLink_emptyTitle() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setTitle("");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");

        ResponseEntity<HttpResponse> response = controller.saveQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Title, URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void saveQuickLink_emptyUrl() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setTitle("Test Title");
        dto.setUrl("");
        dto.setDescription("desc");

        ResponseEntity<HttpResponse> response = controller.saveQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Title, URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void saveQuickLink_emptyDescription() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("");

        ResponseEntity<HttpResponse> response = controller.saveQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Title, URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void saveQuickLink_limitException() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        when(quickLinkService.saveQuickLink(any(QuickLinkRequestDto.class)))
                .thenThrow(new QuickLinkLimitException("Maximum limit reached"));

        ResponseEntity<HttpResponse> response = controller.saveQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Maximum limit reached", response.getBody().getError());
    }

    @Test
    void saveQuickLink_genericException() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        when(quickLinkService.saveQuickLink(any(QuickLinkRequestDto.class)))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<HttpResponse> response = controller.saveQuickLink(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getError().startsWith("Failed to save quick link"));
    }

    @Test
    void getAllQuickLinks_success() {
        QuickLinksResponse result = new QuickLinksResponse();
        result.setQuickLinks(Arrays.asList(new QuickLinkDto()));
        when(quickLinkService.getAllQuickLinks()).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.getAllQuickLinks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void getAllQuickLinks_exception() {
        when(quickLinkService.getAllQuickLinks()).thenThrow(new RuntimeException("fail"));

        ResponseEntity<HttpResponse> response = controller.getAllQuickLinks();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to fetch quick links", response.getBody().getError());
    }

    @Test
    void updateQuickLink_success() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("id1");
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        QuickLinkDto result = new QuickLinkDto();
        result.setLinkId("id1");
        when(quickLinkService.updateQuickLink(any(QuickLinkRequestDto.class))).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.updateQuickLink(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void updateQuickLink_emptyLinkId() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("");
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");

        ResponseEntity<HttpResponse> response = controller.updateQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("LinkId, title, URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void updateQuickLink_emptyTitle() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("id1");
        dto.setTitle("");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");

        ResponseEntity<HttpResponse> response = controller.updateQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("LinkId, title, URL and description must not be empty", response.getBody().getError());
    }

    @Test
    void updateQuickLink_exception() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("id1");
        dto.setTitle("Test Title");
        dto.setUrl("http://test.com");
        dto.setDescription("desc");
        when(quickLinkService.updateQuickLink(any(QuickLinkRequestDto.class)))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<HttpResponse> response = controller.updateQuickLink(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to update quick link", response.getBody().getError());
    }

    @Test
    void deleteQuickLink_success() {
        when(quickLinkService.deleteQuickLink(anyString())).thenReturn(null); // Assuming void, but controller sets data to null

        ResponseEntity<HttpResponse> response = controller.deleteQuickLink("id1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void deleteQuickLink_emptyLinkId() {
        ResponseEntity<HttpResponse> response = controller.deleteQuickLink("");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("linkId must be provided for delete", response.getBody().getError());
    }

    @Test
    void deleteQuickLink_exception() {
        when(quickLinkService.deleteQuickLink(anyString())).thenThrow(new RuntimeException("fail"));

        ResponseEntity<HttpResponse> response = controller.deleteQuickLink("id1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to delete quick link", response.getBody().getError());
    }

    @Test
    void reorderQuickLink_success() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("id1");
        dto.setIndex(1);
        QuickLinksResponse result = new QuickLinksResponse();
        result.setQuickLinks(Arrays.asList(new QuickLinkDto()));
        when(quickLinkService.reorderQuickLink(any(QuickLinkRequestDto.class))).thenReturn(result);

        ResponseEntity<HttpResponse> response = controller.reorderQuickLink(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(result, response.getBody().getData());
        assertNull(response.getBody().getError());
    }

    @Test
    void reorderQuickLink_missingLinkId() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setIndex(1);

        ResponseEntity<HttpResponse> response = controller.reorderQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("linkId and index must be provided for reorder", response.getBody().getError());
    }

    @Test
    void reorderQuickLink_missingIndex() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("id1");

        ResponseEntity<HttpResponse> response = controller.reorderQuickLink(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("linkId and index must be provided for reorder", response.getBody().getError());
    }

    @Test
    void reorderQuickLink_exception() {
        QuickLinkRequestDto dto = new QuickLinkRequestDto();
        dto.setLinkId("id1");
        dto.setIndex(1);
        when(quickLinkService.reorderQuickLink(any(QuickLinkRequestDto.class)))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<HttpResponse> response = controller.reorderQuickLink(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to reorder quick link", response.getBody().getError());
    }
}
package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import com.cognizant.lms.userservice.service.LocaleDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocaleDataControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private LocaleDataService localeDataService;

    private LocaleDataController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new LocaleDataController(localeDataService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testSaveLocaleData_ReturnsOk() throws Exception {
        LocaleRequestDTO dto = new LocaleRequestDTO("en", "home", "{\"k\":\"v\"}");
        doNothing().when(localeDataService).saveLocaleData(any(LocaleRequestDTO.class));

        mockMvc.perform(post("/api/v1/locales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("Data saved successfully"))
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testGetAllLocales_ReturnsList() throws Exception {
        List<LocaleRequestDTO> list = List.of(new LocaleRequestDTO("en", "home", "data"));
        when(localeDataService.getLocaleDataForList()).thenReturn(list);

        mockMvc.perform(get("/api/v1/locales").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].languageCode").value("en"))
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testGetLocalesByLanguage_ReturnsList() throws Exception {
        String lang = "en";
        List<LocaleRequestDTO> list = List.of(new LocaleRequestDTO(lang, "home", "data"));
        when(localeDataService.getLocaleDataByLanguageCode(lang)).thenReturn(list);

        mockMvc.perform(get("/api/v1/locales/{languageCode}", lang).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].languageCode").value(lang))
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testGetLocalesByPageName_ReturnsList() throws Exception {
        String pageName = "Dashboard";
        List<LocaleRequestDTO> list = List.of(
            new LocaleRequestDTO("en", pageName, "{\"greeting\": \"Hello\"}"),
            new LocaleRequestDTO("fr", pageName, "{\"greeting\": \"Bonjour\"}")
        );
        when(localeDataService.getLocaleDataByPageName(pageName)).thenReturn(list);

        mockMvc.perform(get("/api/v1/locales/pages/{pageName}", pageName)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].pageName").value(pageName))
            .andExpect(jsonPath("$.data[0].languageCode").value("en"))
            .andExpect(jsonPath("$.data[1].languageCode").value("fr"))
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testGetLocalesByPageName_ReturnsEmptyList() throws Exception {
        String pageName = "NonExistentPage";
        when(localeDataService.getLocaleDataByPageName(pageName)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/locales/pages/{pageName}", pageName)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testGetLocalesByPageName_WithSpecialCharacters() throws Exception {
        String pageName = "Common-Main";
        List<LocaleRequestDTO> list = List.of(
            new LocaleRequestDTO("en", pageName, "{\"data\": \"test\"}")
        );
        when(localeDataService.getLocaleDataByPageName(pageName)).thenReturn(list);

        mockMvc.perform(get("/api/v1/locales/pages/{pageName}", pageName)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].pageName").value(pageName))
            .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testGetLocalesByPageName_ServiceThrowsException() throws Exception {
        String pageName = "Dashboard";
        when(localeDataService.getLocaleDataByPageName(pageName))
            .thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/v1/locales/pages/{pageName}", pageName)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.error").value("Database connection error"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testDeleteLocaleData_ReturnsOk() throws Exception {
        doNothing().when(localeDataService).deleteLocaleData(anyString(), anyString());

        mockMvc.perform(delete("/api/v1/locales")
                .param("languageCode", "en")
                .param("pageName", "home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("Data deleted successfully"))
            .andExpect(jsonPath("$.status").value(200));
    }
}

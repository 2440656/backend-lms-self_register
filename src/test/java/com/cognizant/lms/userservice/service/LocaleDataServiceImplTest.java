package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.LocaleDataDao;
import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

class LocaleDataServiceImplTest {

    @Mock
    private LocaleDataDao localeDataDao;

    private LocaleDataServiceImpl localeDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        localeDataService = new LocaleDataServiceImpl(localeDataDao);
    }

    @Test
    void testSaveLocaleData_delegatesToDao() {
        LocaleRequestDTO dto = new LocaleRequestDTO("en", "home", "{\"k\":\"v\"}");
        doNothing().when(localeDataDao).saveLocaleData(dto);

        localeDataService.saveLocaleData(dto);

        verify(localeDataDao, times(1)).saveLocaleData(dto);
    }

    @Test
    void testGetLocaleDataForList_returnsDaoResult() {
        List<LocaleRequestDTO> expected = List.of(new LocaleRequestDTO("en", "home", "data"));
        when(localeDataDao.getLocaleDataForList()).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataForList();

        assertNotNull(actual);
        assertEquals(expected, actual);
        verify(localeDataDao, times(1)).getLocaleDataForList();
    }

    @Test
    void testGetLocaleDataByLanguageCode_returnsDaoResult() {
        String lang = "en";
        List<LocaleRequestDTO> expected = List.of(new LocaleRequestDTO("en", "home", "data"));
        when(localeDataDao.getLocaleDataByLanguageCode(lang)).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByLanguageCode(lang);

        assertNotNull(actual);
        assertEquals(expected, actual);
        verify(localeDataDao, times(1)).getLocaleDataByLanguageCode(lang);
    }

    @Test
    void testGetLocaleDataByPageName_returnsMultipleLocalesForSamePage() {
        String pageName = "Dashboard";
        List<LocaleRequestDTO> expected = List.of(
            new LocaleRequestDTO("en", pageName, "{\"greeting\": \"Hello\"}"),
            new LocaleRequestDTO("fr", pageName, "{\"greeting\": \"Bonjour\"}"),
            new LocaleRequestDTO("es", pageName, "{\"greeting\": \"Hola\"}")
        );
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByPageName(pageName);

        assertNotNull(actual);
        assertEquals(3, actual.size());
        assertEquals(expected, actual);
        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_returnsSingleLocale() {
        String pageName = "Login";
        List<LocaleRequestDTO> expected = List.of(
            new LocaleRequestDTO("en", pageName, "{\"title\": \"Login Page\"}")
        );
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByPageName(pageName);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("Login", actual.get(0).getPageName());
        assertEquals("en", actual.get(0).getLanguageCode());
        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_returnsEmptyListWhenPageNotFound() {
        String pageName = "NonExistentPage";
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(List.of());

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByPageName(pageName);

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_handlesNullLocaleData() {
        String pageName = "Settings";
        List<LocaleRequestDTO> expected = List.of(
            new LocaleRequestDTO("en", pageName, null)
        );
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByPageName(pageName);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertNull(actual.get(0).getLocaleData());
        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_handlesPageNameWithSpecialCharacters() {
        String pageName = "Common-Main";
        List<LocaleRequestDTO> expected = List.of(
            new LocaleRequestDTO("en", pageName, "{\"data\": \"test\"}")
        );
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByPageName(pageName);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("Common-Main", actual.get(0).getPageName());
        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_handlesComplexJsonData() {
        String pageName = "Dashboard";
        String complexJson = "{\"greeting\":{\"morning\":\"Good morning\",\"evening\":\"Good evening\"},\"actions\":[\"view\",\"edit\",\"delete\"]}";
        List<LocaleRequestDTO> expected = List.of(
            new LocaleRequestDTO("en", pageName, complexJson)
        );
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(expected);

        List<LocaleRequestDTO> actual = localeDataService.getLocaleDataByPageName(pageName);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(complexJson, actual.get(0).getLocaleData());
        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_throwsExceptionWhenDaoFails() {
        String pageName = "Dashboard";
        when(localeDataDao.getLocaleDataByPageName(pageName))
            .thenThrow(new RuntimeException("Database connection error"));

        assertThrows(RuntimeException.class, () -> {
            localeDataService.getLocaleDataByPageName(pageName);
        });

        verify(localeDataDao, times(1)).getLocaleDataByPageName(pageName);
    }

    @Test
    void testGetLocaleDataByPageName_verifyDaoCalledWithCorrectParameter() {
        String pageName = "ProfilePage";
        List<LocaleRequestDTO> expected = List.of(
            new LocaleRequestDTO("en", pageName, "{}")
        );
        when(localeDataDao.getLocaleDataByPageName(pageName)).thenReturn(expected);

        localeDataService.getLocaleDataByPageName(pageName);

        verify(localeDataDao, times(1)).getLocaleDataByPageName(eq(pageName));
        verify(localeDataDao, never()).getLocaleDataByPageName(argThat(arg -> !arg.equals(pageName)));
    }

    @Test
    void testDeleteLocaleData_delegatesToDao() {
        doNothing().when(localeDataDao).deleteLocaleData("en", "home");

        localeDataService.deleteLocaleData("en", "home");

        verify(localeDataDao, times(1)).deleteLocaleData("en", "home");
    }
}


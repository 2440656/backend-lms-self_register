package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserGlobalSearchHistoryDao;
import com.cognizant.lms.userservice.domain.UserGlobalSearchHistory;
import com.cognizant.lms.userservice.dto.RecentSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGlobalSearchHistoryServiceImplTest {

    @Mock
    private UserGlobalSearchHistoryDao userGlobalSearchHistoryDao;

    @InjectMocks
    private UserGlobalSearchHistoryServiceImpl service;

    @Captor
    private ArgumentCaptor<UserGlobalSearchHistory> searchHistoryCaptor;

    private static final String USER_ID = "user-123";

    private RecentSearchRequest buildRequest(String query) {
        return new RecentSearchRequest(USER_ID, Constants.USER_KEYWORD, query);
    }

    private UserGlobalSearchHistory buildSearch(String keywordOriginal, String keywordNormal, String gsiSk, String active) {
        UserGlobalSearchHistory h = new UserGlobalSearchHistory();
        h.setPk(Constants.USER_PK + Constants.HASH + USER_ID);
        h.setSk(Constants.KEYWORD + Constants.HASH + computeHash(keywordOriginal));
        h.setType(Constants.USER_KEYWORD);
        h.setKeywordOriginal(keywordOriginal);
        h.setKeywordNormal(keywordNormal);
        h.setActive(active);
        h.setGsiSk(gsiSk);
        return h;
    }

    private String computeHash(String searchQuery) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(searchQuery.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void resetMocks() {
        clearInvocations(userGlobalSearchHistoryDao);
    }

    @Test
    @DisplayName("saveRecentSearches returns message when query blank")
    void saveRecentSearches_blankQuery() {
        RecentSearchRequest request = buildRequest("   ");
        String result = service.saveRecentSearches(request);
        assertEquals("Search query cannot be null or empty.", result);
        verifyNoInteractions(userGlobalSearchHistoryDao);
    }

    @Test
    @DisplayName("saveRecentSearches updates existing search if present")
    void saveRecentSearches_existingSearch() {
        String query = "Spring Boot"; // original
        String normalized = query.toLowerCase();
        UserGlobalSearchHistory existing = buildSearch(query, normalized, "ACTIVE#Y#TS#111", Constants.NO); // start inactive -> should become active
        when(userGlobalSearchHistoryDao.getRecentSearchesByUser(USER_ID)).thenReturn(List.of(existing));

        String result = service.saveRecentSearches(buildRequest(query));

        assertEquals("Existing search updated successfully.", result);
        verify(userGlobalSearchHistoryDao).getRecentSearchesByUser(USER_ID);
        verify(userGlobalSearchHistoryDao).updateRecentSearch(searchHistoryCaptor.capture());
        UserGlobalSearchHistory updated = searchHistoryCaptor.getValue();
        assertEquals(Constants.YES, updated.getActive());
        assertNotNull(updated.getModifiedAt());
        assertThat(updated.getGsiSk()).startsWith("ACTIVE#Y#TS#");
        // ensure same pk/sk retained
        assertEquals(existing.getPk(), updated.getPk());
        assertEquals(existing.getSk(), updated.getSk());
        verifyNoMoreInteractions(userGlobalSearchHistoryDao);
    }

    @Test
    @DisplayName("saveRecentSearches inserts new search when active searches under limit")
    void saveRecentSearches_newUnderLimit() {
        List<UserGlobalSearchHistory> recent = new ArrayList<>();
        // 9 active searches
        for (int i = 1; i <= 9; i++) {
            recent.add(buildSearch("query" + i, ("query" + i).toLowerCase(), "ACTIVE#Y#TS#" + (100 + i), Constants.YES));
        }
        // plus one inactive (should be ignored for count)
        recent.add(buildSearch("inactiveQuery", "inactivequery", "ACTIVE#N#TS#50", Constants.NO));
        when(userGlobalSearchHistoryDao.getRecentSearchesByUser(USER_ID)).thenReturn(recent);

        String newQuery = "New Interesting Topic";
        String result = service.saveRecentSearches(buildRequest(newQuery));
        assertEquals("Recent search saved successfully.", result);

        verify(userGlobalSearchHistoryDao).getRecentSearchesByUser(USER_ID);
        verify(userGlobalSearchHistoryDao).saveRecentSearch(searchHistoryCaptor.capture());
        UserGlobalSearchHistory inserted = searchHistoryCaptor.getValue();
        assertEquals(newQuery, inserted.getKeywordOriginal());
        assertEquals(newQuery.toLowerCase(), inserted.getKeywordNormal());
        assertEquals(Constants.YES, inserted.getActive());
        assertThat(inserted.getGsiSk()).startsWith("ACTIVE#Y#TS#");
        String expectedSk = Constants.KEYWORD + Constants.HASH + computeHash(newQuery);
        assertEquals(expectedSk, inserted.getSk());
        verify(userGlobalSearchHistoryDao, never()).updateRecentSearch(any());
    }

    @Test
    @DisplayName("saveRecentSearches deactivates oldest and inserts new when active searches >= limit")
    void saveRecentSearches_overLimit() {
        List<UserGlobalSearchHistory> recent = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            recent.add(buildSearch("query" + i, ("query" + i).toLowerCase(), "ACTIVE#Y#TS#" + (100 + i), Constants.YES));
        }
        when(userGlobalSearchHistoryDao.getRecentSearchesByUser(USER_ID)).thenReturn(recent);

        String newQuery = "Brand New";
        String result = service.saveRecentSearches(buildRequest(newQuery));
        assertEquals("Recent search saved successfully.", result);

        verify(userGlobalSearchHistoryDao).getRecentSearchesByUser(USER_ID);
        verify(userGlobalSearchHistoryDao).updateRecentSearch(searchHistoryCaptor.capture());
        verify(userGlobalSearchHistoryDao).saveRecentSearch(any(UserGlobalSearchHistory.class));

        List<UserGlobalSearchHistory> updatedItems = searchHistoryCaptor.getAllValues();
        UserGlobalSearchHistory deactivated = updatedItems.get(0);
        assertEquals(Constants.NO, deactivated.getActive());
        assertThat(deactivated.getGsiSk()).startsWith("ACTIVE#N#TS#");

    }

    @Test
    @DisplayName("saveRecentSearches wraps exception thrown by DAO")
    void saveRecentSearches_daoThrows() {
        when(userGlobalSearchHistoryDao.getRecentSearchesByUser(USER_ID)).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.saveRecentSearches(buildRequest("abc")));
        assertThat(ex.getMessage()).contains("Failed to save recent search: boom");
        verify(userGlobalSearchHistoryDao).getRecentSearchesByUser(USER_ID);
    }

    @Test
    @DisplayName("updateSearchQueryStatus success path")
    void updateSearchQueryStatus_success() {
        when(userGlobalSearchHistoryDao.deleteRecentSearch(anyString(), anyString())).thenReturn(true);
        service.updateSearchQueryStatus(USER_ID, "Some Query");
        verify(userGlobalSearchHistoryDao).deleteRecentSearch(anyString(), anyString());
    }

    @Test
    @DisplayName("updateSearchQueryStatus failure throws")
    void updateSearchQueryStatus_failure() {
        when(userGlobalSearchHistoryDao.deleteRecentSearch(anyString(), anyString())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.updateSearchQueryStatus(USER_ID, "Another Query"));
        verify(userGlobalSearchHistoryDao).deleteRecentSearch(anyString(), anyString());
    }

    @Test
    @DisplayName("getLatestSearches success returns list")
    void getLatestSearches_success() {
        List<String> expected = List.of("A", "B", "C");
        when(userGlobalSearchHistoryDao.getLatestActiveSearches(USER_ID)).thenReturn(expected);
        List<String> result = service.getLatestSearches(USER_ID);
        assertEquals(expected, result);
        verify(userGlobalSearchHistoryDao).getLatestActiveSearches(USER_ID);
    }

    @Test
    @DisplayName("getLatestSearches failure throws")
    void getLatestSearches_failure() {
        when(userGlobalSearchHistoryDao.getLatestActiveSearches(USER_ID)).thenThrow(new RuntimeException("error"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getLatestSearches(USER_ID));
        assertEquals("error", ex.getMessage());
        verify(userGlobalSearchHistoryDao).getLatestActiveSearches(USER_ID);
    }
}


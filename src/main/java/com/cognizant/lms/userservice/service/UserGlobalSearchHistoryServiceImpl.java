package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.UserGlobalSearchHistoryDao;
import com.cognizant.lms.userservice.domain.UserGlobalSearchHistory;
import com.cognizant.lms.userservice.dto.RecentSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserGlobalSearchHistoryServiceImpl  implements  UserGlobalSearchHistoryService {

  private final UserGlobalSearchHistoryDao userGlobalSearchHistoryDao;

  public UserGlobalSearchHistoryServiceImpl(UserGlobalSearchHistoryDao userGlobalSearchHistoryDao) {
    this.userGlobalSearchHistoryDao = userGlobalSearchHistoryDao;
  }

    @Override
    public String saveRecentSearches(RecentSearchRequest recentSearchRequest) {
        if (recentSearchRequest.getSearchQuery() == null || recentSearchRequest.getSearchQuery().trim().isEmpty()) {
            log.warn("Search query is null or empty for user: {}", recentSearchRequest.getUserId());
            return "Search query cannot be null or empty.";
        }

        String userId = recentSearchRequest.getUserId();
        String searchQueryOriginal = recentSearchRequest.getSearchQuery().trim();
        String searchQueryNormalized = searchQueryOriginal.toLowerCase();
        String hashKeyword = generateHashKeyword(searchQueryOriginal);
        String pk = Constants.USER_PK + Constants.HASH + userId;
        String sk = Constants.KEYWORD + Constants.HASH + hashKeyword;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATED_ON_UPDATED_ON_TIMESTAMP);
        long epochMillis = System.currentTimeMillis();

        try {
            // Fetch all recent searches for user
            List<UserGlobalSearchHistory> recentSearches = userGlobalSearchHistoryDao.getRecentSearchesByUser(userId);
            List<UserGlobalSearchHistory> activeSearches = recentSearches.stream()
                    .filter(s -> Constants.YES.equals(s.getActive()))
                    .sorted(Comparator.comparing(UserGlobalSearchHistory::getGsiSk)) // oldest first
                    .toList();

            // Check if search already exists
            Optional<UserGlobalSearchHistory> existingSearchOpt = recentSearches.stream()
                    .filter(s -> s.getKeywordNormal().equals(searchQueryNormalized))
                    .findFirst();

            if (existingSearchOpt.isPresent()) {
                // Update existing search: active = Y, modifiedAt, gsiSk
                UserGlobalSearchHistory existingSearch = existingSearchOpt.get();
                existingSearch.setActive(Constants.YES);
                existingSearch.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC).format(formatter));
                existingSearch.setGsiSk(Constants.ACTIVE + Constants.HASH + Constants.YES + Constants.HASH +
                        Constants.TIME_STAMP + Constants.HASH + epochMillis);

                userGlobalSearchHistoryDao.updateRecentSearch(existingSearch);
                return "Existing search updated successfully.";
            }

            // Prepare new search object
            UserGlobalSearchHistory newSearch = new UserGlobalSearchHistory();
            newSearch.setPk(pk);
            newSearch.setSk(sk);
            newSearch.setType(Constants.USER_KEYWORD);
            newSearch.setKeywordOriginal(searchQueryOriginal);
            newSearch.setKeywordNormal(searchQueryNormalized);
            newSearch.setActive(Constants.YES);
            newSearch.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).format(formatter));
            newSearch.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC).format(formatter));
            newSearch.setGsiSk(Constants.ACTIVE + Constants.HASH + Constants.YES + Constants.HASH +
                    Constants.TIME_STAMP + Constants.HASH + epochMillis);

            if (activeSearches.size() < 10) {
                // Just insert new search
                userGlobalSearchHistoryDao.saveRecentSearch(newSearch);
            } else {
                // Mark oldest active search as inactive
                UserGlobalSearchHistory oldestSearch = activeSearches.getFirst();
                oldestSearch.setActive(Constants.NO);
                oldestSearch.setGsiSk(Constants.ACTIVE + Constants.HASH + Constants.NO + Constants.HASH +
                        Constants.TIME_STAMP + Constants.HASH + System.currentTimeMillis());
                userGlobalSearchHistoryDao.updateRecentSearch(oldestSearch);

                // Insert new search
                userGlobalSearchHistoryDao.saveRecentSearch(newSearch);
            }

            return "Recent search saved successfully.";
        } catch (Exception e) {
            log.error("Failed to save recent search for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to save recent search: " + e.getMessage(), e);
        }
    }


  @Override
  public void updateSearchQueryStatus(String userId, String searchKeyword) {
    String hashKeyword = generateHashKeyword(searchKeyword);
    String pk = Constants.USER_PK + Constants.HASH + userId;
    String sk = Constants.KEYWORD + Constants.HASH + hashKeyword;

    if (userGlobalSearchHistoryDao.deleteRecentSearch(pk, sk)) {
      log.info("status updated successfully ");
    } else {
      log.error("Failed to update status");
      throw new RuntimeException("Failed to update active status");
    }
  }

  private static String generateHashKeyword(String searchQuery) {
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
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<String> getLatestSearches(String userId) {
    List<String> latestSearches;
    try {
      latestSearches = userGlobalSearchHistoryDao.getLatestActiveSearches(userId);
      log.info("latest searches: {}", latestSearches);
    } catch (Exception e) {
      log.error("Failed to fetch all the latest searches {} ", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
    return latestSearches;
  }

}

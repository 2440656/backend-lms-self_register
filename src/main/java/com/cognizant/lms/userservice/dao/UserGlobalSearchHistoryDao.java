package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.domain.UserGlobalSearchHistory;
import com.cognizant.lms.userservice.dto.RecentSearchRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGlobalSearchHistoryDao {

  void saveRecentSearch(UserGlobalSearchHistory userGlobalSearchHistory);

  boolean isRecentSearchExists(String pk, String sk, String searchQuery);

  boolean deleteRecentSearch(String pk, String sk);

  List<UserGlobalSearchHistory> getRecentSearchesByUser(String userId);

  void updateRecentSearch(UserGlobalSearchHistory updatedSearch);

  List<String> getLatestActiveSearches(String userId);

  void updateSearchStatusAndModifiedAt(String pk, String sk) ;
}

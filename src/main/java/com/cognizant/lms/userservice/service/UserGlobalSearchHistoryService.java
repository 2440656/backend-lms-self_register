package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dto.RecentSearchRequest;

import java.util.List;

public interface UserGlobalSearchHistoryService {

  String saveRecentSearches( RecentSearchRequest recentSearchRequest);

  void updateSearchQueryStatus(String userId,String searchKeyword);

  List<String> getLatestSearches(String userId);
}

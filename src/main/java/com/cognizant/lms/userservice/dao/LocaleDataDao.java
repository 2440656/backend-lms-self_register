package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import java.util.List;

public interface LocaleDataDao {
    void saveLocaleData(LocaleRequestDTO localeRequestDTO);

    List<LocaleRequestDTO> getLocaleDataForList();

    List<LocaleRequestDTO> getLocaleDataByLanguageCode(String languageCode);

    List<LocaleRequestDTO> getLocaleDataByPageName(String pageName);

    void deleteLocaleData(String languageCode, String pageName);
}

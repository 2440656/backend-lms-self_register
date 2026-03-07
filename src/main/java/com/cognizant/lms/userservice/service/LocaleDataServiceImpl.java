package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.dao.LocaleDataDao;
import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LocaleDataServiceImpl implements LocaleDataService {

    private static final Logger logger = LoggerFactory.getLogger(LocaleDataServiceImpl.class);

    private final LocaleDataDao localeDataDao;

    @Autowired
    public LocaleDataServiceImpl(LocaleDataDao localeDataDao) {
        this.localeDataDao = localeDataDao;
        logger.debug("Initialized LocaleDataServiceImpl with dao: {}", localeDataDao.getClass().getSimpleName());
    }

    @Override
    public void saveLocaleData(LocaleRequestDTO localeRequestDTO) {
        logger.info("Service: saving locale data for languageCode={}, pageName={}", localeRequestDTO.getLanguageCode(), localeRequestDTO.getPageName());
        localeDataDao.saveLocaleData(localeRequestDTO);
    }

    @Override
    public List<LocaleRequestDTO> getLocaleDataForList() {
        logger.info("Service: fetching all locale data");
        return localeDataDao.getLocaleDataForList();
    }

    @Override
    public List<LocaleRequestDTO> getLocaleDataByLanguageCode(String languageCode) {
        logger.info("Service: fetching locale data for languageCode={}", languageCode);
        return localeDataDao.getLocaleDataByLanguageCode(languageCode);
    }

    @Override
    public List<LocaleRequestDTO> getLocaleDataByPageName(String pageName) {
        logger.info("Service: fetching locale data for pageName={}", pageName);
        return localeDataDao.getLocaleDataByPageName(pageName);
    }


    @Override
    public void deleteLocaleData(String languageCode, String pageName) {
        logger.info("Service: deleting locale data for languageCode={}, pageName={}", languageCode, pageName);
        localeDataDao.deleteLocaleData(languageCode, pageName);
    }
}

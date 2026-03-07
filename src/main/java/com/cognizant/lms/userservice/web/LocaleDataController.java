package com.cognizant.lms.userservice.web;

import com.cognizant.lms.userservice.dto.HttpResponse;
import com.cognizant.lms.userservice.dto.LocaleRequestDTO;
import com.cognizant.lms.userservice.service.LocaleDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/locales")
public class LocaleDataController {

    private static final Logger logger = LoggerFactory.getLogger(LocaleDataController.class);

    private final LocaleDataService localeDataService;

    @Autowired
    public LocaleDataController(LocaleDataService localeDataService) {
        this.localeDataService = localeDataService;
        logger.debug("Initialized LocaleDataController with service: {}", localeDataService.getClass().getSimpleName());
    }

    @PostMapping
    public ResponseEntity<HttpResponse> saveLocaleData(@RequestBody LocaleRequestDTO request) {
        HttpResponse httpResponse = new HttpResponse();
        try {
            logger.info("Received request to save locale data for languageCode={}, pageName={}", request.getLanguageCode(), request.getPageName());
            localeDataService.saveLocaleData(request);
            httpResponse.setData("Data saved successfully");
            httpResponse.setStatus(HttpStatus.OK.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        } catch (Exception exception) {
            httpResponse.setData(null);
            httpResponse.setError(exception.getMessage());
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        }
    }

    @GetMapping
    public ResponseEntity<HttpResponse> getAllLocales() {
        HttpResponse httpResponse = new HttpResponse();
        try {
            logger.info("Received request to fetch all locale data");
            var data = localeDataService.getLocaleDataForList();
            httpResponse.setData(data);
            httpResponse.setStatus(HttpStatus.OK.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        } catch (Exception exception) {
            httpResponse.setData(null);
            httpResponse.setError(exception.getMessage());
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        }
    }

    @GetMapping("/{languageCode}")
    public ResponseEntity<HttpResponse> getLocalesByLanguage(@PathVariable String languageCode) {
        HttpResponse httpResponse = new HttpResponse();
        try {
            logger.info("Received request to fetch locale data for languageCode={}", languageCode);
            var data = localeDataService.getLocaleDataByLanguageCode(languageCode);
            httpResponse.setData(data);
            httpResponse.setStatus(HttpStatus.OK.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        } catch (Exception exception) {
            httpResponse.setData(null);
            httpResponse.setError(exception.getMessage());
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        }
    }

    @GetMapping("/pages/{pageName}")
    public ResponseEntity<HttpResponse> getLocalesByPageName(@PathVariable String pageName) {
        HttpResponse httpResponse = new HttpResponse();
        try {
            logger.info("Received request to fetch locale data for pageName={}", pageName);
            var data = localeDataService.getLocaleDataByPageName(pageName);
            httpResponse.setData(data);
            httpResponse.setStatus(HttpStatus.OK.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        } catch (Exception exception) {
            httpResponse.setData(null);
            httpResponse.setError(exception.getMessage());
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        }
    }

    @DeleteMapping
    public ResponseEntity<HttpResponse> deleteLocaleData(@RequestParam String languageCode, @RequestParam String pageName) {
        HttpResponse httpResponse = new HttpResponse();
        try {
            logger.info("Received request to delete locale data for languageCode={}, pageName={}", languageCode, pageName);
            localeDataService.deleteLocaleData(languageCode, pageName);
            httpResponse.setData("Data deleted successfully");
            httpResponse.setStatus(HttpStatus.OK.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        } catch (Exception exception) {
            httpResponse.setData(null);
            httpResponse.setError(exception.getMessage());
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(httpResponse.getStatus()).body(httpResponse);
        }
    }
}

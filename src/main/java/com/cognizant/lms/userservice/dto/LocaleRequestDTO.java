package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocaleRequestDTO {
    private String languageCode;
    private String pageName;
    private String localeData;
}

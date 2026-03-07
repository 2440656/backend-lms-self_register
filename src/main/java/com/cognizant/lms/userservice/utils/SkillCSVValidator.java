package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.constants.Constants;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;


@Component
public class SkillCSVValidator {

    public boolean validateHeaders(Map<String, Integer> headers, List<String> validHeaders) {
        return headers.keySet().containsAll(validHeaders) && new HashSet<>(validHeaders).containsAll(headers.keySet());
    }

    public List<String> validateSkillsFields(CSVRecord record, int rowNum, Set<String> seenSkillCodes) {
        List<String> fieldErrors = new ArrayList<>();
        for (String columnHeader : record.toMap().keySet()) {
            switch (columnHeader) {
                case "skillCode" -> {
                    if (record.get(columnHeader) == null || record.get(columnHeader).trim().isEmpty()){
                        fieldErrors.add(columnHeader + " is mandatory in row " + rowNum);
                    }
                    else if (record.get(columnHeader).length() > 90){
                        fieldErrors.add(columnHeader + " exceeds 90 chars in row " + rowNum);
                    }
                    else if (!record.get(columnHeader).matches("[a-zA-Z0-9 ]+")) {
                        fieldErrors.add(columnHeader + " must contain only letters, numbers, and spaces in row " + rowNum);
                    }
                }
                case "skillName" -> {
                    if (record.get(columnHeader) == null || record.get(columnHeader).trim().isEmpty()) {
                        fieldErrors.add(columnHeader + " is mandatory in row " + rowNum);
                    }
                    else if (record.get(columnHeader).length() > 255){
                        fieldErrors.add(columnHeader + " exceeds 255 chars in row " + rowNum);
                    }
                    else if(!record.get(columnHeader).matches(Constants.SKILL_NAME_REGEX)) {
                        fieldErrors.add(columnHeader + " must contain only letters, numbers, and special characters in row " + rowNum);
                    }
                }
                case "skillType" -> {
                    if (record.get(columnHeader) == null || record.get(columnHeader).trim().isEmpty()){
                        fieldErrors.add(columnHeader + " is mandatory in row " + rowNum);
                    }
                    else if (record.get(columnHeader).length() > 255) {
                        fieldErrors.add(columnHeader + " exceeds 255 chars in row " + rowNum);
                    }
                    else if (!record.get(columnHeader).matches("^[a-zA-Z ]+$")) {
                        fieldErrors.add(columnHeader + " must contain only letters and spaces in row " + rowNum);
                    }
                }
                case "status" -> {
                    if (record.get(columnHeader) == null || record.get(columnHeader).trim().isEmpty()){
                        fieldErrors.add(columnHeader + " is mandatory in row " + rowNum);
                    }
                    else if (record.get(columnHeader).length() > 25){
                        fieldErrors.add(columnHeader + " exceeds 25 chars in row " + rowNum);
                    }
                    else if (!record.get(columnHeader).matches("^[a-zA-Z ]+$")){
                        fieldErrors.add(columnHeader + " must contain only letters and spaces in row " + rowNum);
                    }
                    else if (!record.get(columnHeader).equalsIgnoreCase("Active") && !record.get(columnHeader).equalsIgnoreCase("Inactive")) {
                        fieldErrors.add(columnHeader + " must be either 'Active' or 'Inactive' in row " + rowNum);
                    }
                }
                case "skillDescription" -> {
                    if (record.get(columnHeader) != null && record.get(columnHeader).length() > 2000) {
                        fieldErrors.add(columnHeader + " exceeds 2000 chars in row " + rowNum);
                    }
                    else if (record.get(columnHeader) != null && !record.get(columnHeader).trim().isEmpty() && !record.get(columnHeader).matches(Constants.SKILL_NAME_REGEX)) {
                        fieldErrors.add(columnHeader + " must contain only letters, numbers, and special characters in row " + rowNum);
                    }
                }
                case "skillCategory" -> {
                    if (record.get(columnHeader) != null && record.get(columnHeader).length() > 255){
                        fieldErrors.add(columnHeader + " exceeds 255 chars in row " + rowNum);
                    }
                    else if (record.get(columnHeader) != null && !record.get(columnHeader).trim().isEmpty() && !record.get(columnHeader).matches(Constants.SKILL_NAME_REGEX)) {
                        fieldErrors.add(columnHeader + " must contain only letters, numbers, and special characters in row " + rowNum);
                    }
                }
                case "skillSubCategory" -> {
                    String skillSubCategory = record.get(columnHeader);
                    if (skillSubCategory != null && skillSubCategory.length() > 255){
                        fieldErrors.add(columnHeader + " exceeds 255 chars in row " + rowNum);
                    }
                    else if (skillSubCategory != null && !skillSubCategory.trim().isEmpty() && !skillSubCategory.matches(Constants.SKILL_NAME_REGEX)) {
                        fieldErrors.add(columnHeader + " must contain only letters, numbers, and special characters in row " + rowNum);
                    }
                }
                default -> {
                    // Ignore unknown headers
                }
            }
        }
        return fieldErrors;
    }


}

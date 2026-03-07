package com.cognizant.lms.userservice.exception;

public class UserSettingAlreadyFoundException extends RuntimeException {
    public UserSettingAlreadyFoundException(String message) {
        super(message);
    }
}

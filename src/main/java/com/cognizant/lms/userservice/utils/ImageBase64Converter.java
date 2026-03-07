package com.cognizant.lms.userservice.utils;

import java.io.IOException;
import java.util.Base64;

public class ImageBase64Converter {
  public static String convertImageToBase64(byte[] imageBytes) throws IOException {
    return Base64.getEncoder().encodeToString(imageBytes);
  }
}

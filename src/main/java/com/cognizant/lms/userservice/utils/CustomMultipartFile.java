package com.cognizant.lms.userservice.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class CustomMultipartFile implements MultipartFile {
  private byte[] input;
  private String fileName;

  public CustomMultipartFile(List<String> stringList, String fileName) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (String str : stringList) {
      sb.append(str).append(System.lineSeparator());
    }
    this.input = sb.toString().getBytes(StandardCharsets.UTF_8);
    this.fileName = fileName;
  }

  @Override
  public String getName() {
    return fileName;
  }

  @Override
  public String getOriginalFilename() {
    return fileName;
  }

  @Override
  public String getContentType() {
    return "text/plain";
  }

  @Override
  public boolean isEmpty() {
    return input == null || input.length == 0;
  }

  @Override
  public long getSize() {
    return input.length;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return input;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(input);
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    try (FileOutputStream fos = new FileOutputStream(dest)) {
      fos.write(input);
    }
  }
}

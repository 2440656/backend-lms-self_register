package com.cognizant.lms.userservice.utils;

public enum BucketPath {
  IMAGES("/images"),
  TEMPLATES("/templates"),
  UPLOADS("/uploads");

  private final String path;

  BucketPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}
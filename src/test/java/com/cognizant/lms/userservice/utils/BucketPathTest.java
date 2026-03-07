package com.cognizant.lms.userservice.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BucketPathTest {

  @Test
  public void testGetPath() {
    assertEquals("/images", BucketPath.IMAGES.getPath());
    assertEquals("/templates", BucketPath.TEMPLATES.getPath());
    assertEquals("/uploads", BucketPath.UPLOADS.getPath());
  }

  @Test
  public void testEnumValues() {
    BucketPath[] expectedValues = {BucketPath.IMAGES, BucketPath.TEMPLATES, BucketPath.UPLOADS};
    BucketPath[] actualValues = BucketPath.values();
    assertArrayEquals(expectedValues, actualValues);
  }

  @Test
  public void testValueOf() {
    assertEquals(BucketPath.IMAGES, BucketPath.valueOf("IMAGES"));
    assertEquals(BucketPath.TEMPLATES, BucketPath.valueOf("TEMPLATES"));
    assertEquals(BucketPath.UPLOADS, BucketPath.valueOf("UPLOADS"));
  }
}


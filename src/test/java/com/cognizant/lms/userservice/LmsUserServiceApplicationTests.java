package com.cognizant.lms.userservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "/application.properties")
class LmsUserServiceApplicationTests {
  @Test
  void contextLoads() {
  }

  @MockBean
  LmsUserServiceApplication lmsUserServiceApplication;
}





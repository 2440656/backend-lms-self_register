package com.cognizant.lms.userservice;

import com.cognizant.lms.userservice.web.UserController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Import({UserController.class})
@EnableAutoConfiguration
@Async
public class LmsUserServiceApplication {

  @Value("${CORS_URL}")
  private String corsUrl;

  public static void main(String[] args) {
    SpringApplication.run(LmsUserServiceApplication.class, args);
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) { 
        registry.addMapping("/api/v1/**")
            .allowedOriginPatterns(corsUrl.split(","))
            .allowedMethods("*").allowCredentials(true);
      }
    };
  }

}

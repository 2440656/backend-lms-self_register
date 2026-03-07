package com.cognizant.lms.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // You can customize the RestTemplate here if needed
        // For example, you can add interceptors, message converters, etc.
        // RestTemplate restTemplate = new RestTemplate();
        // restTemplate.getInterceptors().add(new YourCustomInterceptor());
        // return restTemplate;

        // Return a default RestTemplate instance
      return new RestTemplate();
    }
}
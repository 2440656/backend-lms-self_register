package com.cognizant.lms.userservice.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/servicestatus")
public class ServiceStatusController {
  @GetMapping
  public ResponseEntity<Void> status() {
    return ResponseEntity.ok().build();

  }
}

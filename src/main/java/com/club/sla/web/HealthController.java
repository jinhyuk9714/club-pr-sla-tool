package com.club.sla.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  private final ApplicationHealthService applicationHealthService;

  public HealthController(ApplicationHealthService applicationHealthService) {
    this.applicationHealthService = applicationHealthService;
  }

  @GetMapping("/health")
  public ResponseEntity<ApplicationHealthSnapshot> health() {
    ApplicationHealthSnapshot snapshot = applicationHealthService.currentHealth();
    HttpStatus status =
        "UP".equals(snapshot.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return ResponseEntity.status(status).body(snapshot);
  }
}

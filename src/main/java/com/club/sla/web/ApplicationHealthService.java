package com.club.sla.web;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApplicationHealthService {

  private final JdbcTemplate jdbcTemplate;
  private final Flyway flyway;

  public ApplicationHealthService(JdbcTemplate jdbcTemplate, Flyway flyway) {
    this.jdbcTemplate = jdbcTemplate;
    this.flyway = flyway;
  }

  public ApplicationHealthSnapshot currentHealth() {
    if (!databaseUp()) {
      return new ApplicationHealthSnapshot("DOWN", "DOWN", "DOWN");
    }
    if (migrationsPending()) {
      return new ApplicationHealthSnapshot("DOWN", "UP", "DOWN");
    }
    return new ApplicationHealthSnapshot("UP", "UP", "UP");
  }

  private boolean databaseUp() {
    try {
      Integer probe = jdbcTemplate.queryForObject("select 1", Integer.class);
      return probe != null && probe == 1;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private boolean migrationsPending() {
    return flyway.info().pending().length > 0;
  }
}

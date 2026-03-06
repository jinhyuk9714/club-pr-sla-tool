package com.club.sla.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

class ApplicationHealthServiceTest {

  @Test
  void reportsUpWhenDatabaseRespondsAndNoPendingMigrationsRemain() {
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    Flyway flyway = Mockito.mock(Flyway.class);
    MigrationInfoService migrationInfoService = Mockito.mock(MigrationInfoService.class);
    given(jdbcTemplate.queryForObject("select 1", Integer.class)).willReturn(1);
    given(flyway.info()).willReturn(migrationInfoService);
    given(migrationInfoService.pending()).willReturn(new MigrationInfo[0]);

    ApplicationHealthService applicationHealthService =
        new ApplicationHealthService(jdbcTemplate, flyway);

    assertThat(applicationHealthService.currentHealth())
        .isEqualTo(new ApplicationHealthSnapshot("UP", "UP", "UP"));
  }

  @Test
  void reportsDownWhenPendingMigrationsRemain() {
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    Flyway flyway = Mockito.mock(Flyway.class);
    MigrationInfoService migrationInfoService = Mockito.mock(MigrationInfoService.class);
    given(jdbcTemplate.queryForObject("select 1", Integer.class)).willReturn(1);
    given(flyway.info()).willReturn(migrationInfoService);
    given(migrationInfoService.pending())
        .willReturn(new MigrationInfo[] {Mockito.mock(MigrationInfo.class)});

    ApplicationHealthService applicationHealthService =
        new ApplicationHealthService(jdbcTemplate, flyway);

    assertThat(applicationHealthService.currentHealth())
        .isEqualTo(new ApplicationHealthSnapshot("DOWN", "UP", "DOWN"));
  }

  @Test
  void reportsDownWhenDatabaseProbeFails() {
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    Flyway flyway = Mockito.mock(Flyway.class);
    given(jdbcTemplate.queryForObject("select 1", Integer.class))
        .willThrow(new IllegalStateException("db down"));

    ApplicationHealthService applicationHealthService =
        new ApplicationHealthService(jdbcTemplate, flyway);

    assertThat(applicationHealthService.currentHealth())
        .isEqualTo(new ApplicationHealthSnapshot("DOWN", "DOWN", "DOWN"));
  }
}

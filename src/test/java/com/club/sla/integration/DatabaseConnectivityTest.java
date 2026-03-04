package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class DatabaseConnectivityTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("club_sla")
          .withUsername("club_sla")
          .withPassword("club_sla");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
  }

  @Autowired private DataSource dataSource;

  @Test
  void connectsToDatabaseAndHasFlywayHistoryTable() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
    Integer flywayTableExists =
        jdbcTemplate.queryForObject(
            """
            select count(*) from information_schema.tables
            where table_schema = 'public' and table_name = 'flyway_schema_history'
            """,
            Integer.class);

    assertThat(one).isEqualTo(1);
    assertThat(flywayTableExists).isEqualTo(1);
  }
}

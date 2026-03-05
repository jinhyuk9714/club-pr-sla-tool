package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.sla.PostgresAdvisoryLockService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PostgresAdvisoryLockServiceIntegrationTest {

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
  void acquiresAndReleasesAdvisoryLock() {
    PostgresAdvisoryLockService firstLockService = new PostgresAdvisoryLockService(dataSource);
    PostgresAdvisoryLockService secondLockService = new PostgresAdvisoryLockService(dataSource);

    boolean firstLock = firstLockService.tryLock("sla-scan");
    boolean secondLockWhileHeld = secondLockService.tryLock("sla-scan");
    firstLockService.unlock("sla-scan");
    boolean secondLockAfterRelease = secondLockService.tryLock("sla-scan");
    secondLockService.unlock("sla-scan");

    assertThat(firstLock).isTrue();
    assertThat(secondLockWhileHeld).isFalse();
    assertThat(secondLockAfterRelease).isTrue();
  }
}

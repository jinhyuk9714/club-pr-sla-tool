package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLog;
import com.club.sla.sla.SlaEventLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SlaEventLogRepositoryIntegrationTest {

  private static final String TEST_DB_NAME = "club_sla";
  private static final String TEST_DB_USER = "club_sla";
  private static final String TEST_DB_CREDENTIAL = UUID.randomUUID().toString();

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName(TEST_DB_NAME)
          .withUsername(TEST_DB_USER)
          .withPassword(TEST_DB_CREDENTIAL);

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
  }

  @Autowired private SlaEventLogRepository slaEventLogRepository;

  @BeforeEach
  void clearData() {
    slaEventLogRepository.deleteAll();
  }

  @Test
  void enforcesUniqueRepoPrStageConstraint() {
    slaEventLogRepository.save(new SlaEventLog(1L, 101L, SlaAction.REMIND_12H, Instant.now()));

    assertThatThrownBy(
            () ->
                slaEventLogRepository.saveAndFlush(
                    new SlaEventLog(1L, 101L, SlaAction.REMIND_12H, Instant.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void allowsDifferentStagesForSamePullRequest() {
    slaEventLogRepository.saveAndFlush(
        new SlaEventLog(1L, 101L, SlaAction.REMIND_12H, Instant.now()));
    slaEventLogRepository.saveAndFlush(
        new SlaEventLog(1L, 101L, SlaAction.ESCALATE_24H, Instant.now()));

    assertThat(slaEventLogRepository.count()).isEqualTo(2);
  }
}

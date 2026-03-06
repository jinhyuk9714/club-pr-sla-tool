package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.audit.AdminAuditLog;
import com.club.sla.audit.AdminAuditLogRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class AdminAuditLogRepositoryIntegrationTest {

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

  @Autowired private AdminAuditLogRepository adminAuditLogRepository;

  @BeforeEach
  void setUp() {
    adminAuditLogRepository.deleteAll();
  }

  @Test
  void savesAndQueriesLogsByOperationAndCreatedAtDesc() {
    AdminAuditLog first =
        adminAuditLogRepository.saveAndFlush(
            new AdminAuditLog(
                "DEAD_LETTER_LIST",
                "/api/admin/dead-letters",
                "GET",
                200,
                "SUCCESS",
                null,
                null,
                null,
                null,
                Instant.parse("2026-03-06T01:00:00Z")));

    AdminAuditLog second =
        adminAuditLogRepository.saveAndFlush(
            new AdminAuditLog(
                "SLA_REEVALUATE",
                "/api/repositories/1/pull-requests/2/sla/re-evaluate",
                "POST",
                404,
                "NOT_FOUND",
                1L,
                2L,
                null,
                "NOT_FOUND",
                Instant.parse("2026-03-06T02:00:00Z")));

    List<AdminAuditLog> allRows =
        adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
    List<AdminAuditLog> reevaluateRows =
        adminAuditLogRepository.findByOperationOrderByCreatedAtDesc(
            "SLA_REEVALUATE", PageRequest.of(0, 10));

    assertThat(first.getId()).isNotNull();
    assertThat(second.getId()).isNotNull();
    assertThat(allRows).hasSize(2);
    assertThat(allRows.get(0).getOperation()).isEqualTo("SLA_REEVALUATE");
    assertThat(allRows.get(1).getOperation()).isEqualTo("DEAD_LETTER_LIST");
    assertThat(reevaluateRows).hasSize(1);
    assertThat(reevaluateRows.get(0).getHttpStatus()).isEqualTo(404);
  }
}

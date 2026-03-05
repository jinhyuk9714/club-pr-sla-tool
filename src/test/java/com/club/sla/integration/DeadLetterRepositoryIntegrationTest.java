package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.notify.DeadLetterEvent;
import com.club.sla.notify.DeadLetterRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
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
class DeadLetterRepositoryIntegrationTest {

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

  @Autowired private DeadLetterRepository deadLetterRepository;

  @BeforeEach
  void clearData() {
    deadLetterRepository.deleteAll();
  }

  @Test
  void storesAndLoadsDeadLetter() {
    DeadLetterEvent saved =
        deadLetterRepository.saveAndFlush(
            new DeadLetterEvent("DISCORD_SEND_FAILED", "{\"repoId\":1}", Instant.now()));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getReason()).isEqualTo("DISCORD_SEND_FAILED");
    assertThat(saved.getPayload()).contains("repoId");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(deadLetterRepository.count()).isEqualTo(1);
  }
}

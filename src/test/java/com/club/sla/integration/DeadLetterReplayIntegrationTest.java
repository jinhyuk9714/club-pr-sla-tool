package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.club.sla.delivery.OutboundDeliveryJobRepository;
import com.club.sla.delivery.OutboundDeliveryJobService;
import com.club.sla.notify.DeadLetterEvent;
import com.club.sla.notify.DeadLetterReplayService;
import com.club.sla.notify.DeadLetterReplayStatus;
import com.club.sla.notify.DeadLetterRepository;
import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLogRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class DeadLetterReplayIntegrationTest {

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
  @Autowired private DeadLetterReplayService deadLetterReplayService;
  @Autowired private SlaEventLogRepository slaEventLogRepository;
  @Autowired private OutboundDeliveryJobRepository outboundDeliveryJobRepository;

  @SpyBean private OutboundDeliveryJobService outboundDeliveryJobService;

  @BeforeEach
  void setUp() {
    slaEventLogRepository.deleteAll();
    deadLetterRepository.deleteAll();
    outboundDeliveryJobRepository.deleteAll();
  }

  @Test
  void updatesStatusToReplayedWhenReplaySucceeds() {
    DeadLetterEvent deadLetter =
        deadLetterRepository.saveAndFlush(
            new DeadLetterEvent(
                "DISCORD_SEND_FAILED",
                "[SLA] repo=50 pr=70 stage=REMIND_12H",
                Instant.now(),
                50L,
                70L,
                SlaAction.REMIND_12H));

    deadLetterReplayService.replay(deadLetter.getId());

    DeadLetterEvent updated = deadLetterRepository.findById(deadLetter.getId()).orElseThrow();
    assertThat(updated.getReplayStatus()).isEqualTo(DeadLetterReplayStatus.REPLAYED);
    assertThat(updated.getReplayedAt()).isNotNull();
    assertThat(outboundDeliveryJobRepository.findAll()).hasSize(1);
  }

  @Test
  void updatesStatusToFailedWhenReplaySendThrows() {
    DeadLetterEvent deadLetter =
        deadLetterRepository.saveAndFlush(
            new DeadLetterEvent(
                "DISCORD_SEND_FAILED",
                "[SLA] repo=51 pr=71 stage=ESCALATE_24H",
                Instant.now(),
                51L,
                71L,
                SlaAction.ESCALATE_24H));
    doThrow(new RuntimeException("enqueue failed"))
        .when(outboundDeliveryJobService)
        .enqueueDiscordNotification(any());

    assertThatThrownBy(() -> deadLetterReplayService.replay(deadLetter.getId()))
        .isInstanceOf(DeadLetterReplayService.DeadLetterReplayFailedException.class);

    DeadLetterEvent updated = deadLetterRepository.findById(deadLetter.getId()).orElseThrow();
    assertThat(updated.getReplayStatus()).isEqualTo(DeadLetterReplayStatus.FAILED);
    assertThat(updated.getReplayAttempts()).isEqualTo(1);
    assertThat(updated.getLastError()).contains("enqueue failed");
    assertThat(outboundDeliveryJobRepository.findAll()).isEmpty();
  }
}

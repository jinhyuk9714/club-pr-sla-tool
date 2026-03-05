package com.club.sla.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.notify.NotificationMessage;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLogRepository;
import com.club.sla.sla.SlaScannerJob;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestNotificationPortConfig.class)
class SlaFlowE2ETest {

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
    registry.add("sla.scanner.initial-delay-ms", () -> 300000);
  }

  @Autowired private PullRequestStateRepository pullRequestStateRepository;
  @Autowired private SlaEventLogRepository slaEventLogRepository;
  @Autowired private SlaScannerJob slaScannerJob;
  @Autowired private MeterRegistry meterRegistry;
  @Autowired private TestNotificationPortConfig.RecordingNotificationPort recordingNotificationPort;

  @BeforeEach
  void setUp() {
    slaEventLogRepository.deleteAll();
    pullRequestStateRepository.deleteAll();
    recordingNotificationPort.clear();
  }

  @Test
  void readyToReviewFlowTriggersReminderEscalationAndStopsAfterReview() {
    PullRequestState state = new PullRequestState(9001L, 77L, "alice");
    state.setStatus(PullRequestStatus.READY);
    state.setReadyAt(Instant.now().minusSeconds(13 * 3600));
    pullRequestStateRepository.saveAndFlush(state);

    slaScannerJob.scan();
    assertThat(recordingNotificationPort.allMessages())
        .containsExactly(new NotificationMessage(9001L, 77L, SlaAction.REMIND_12H));
    assertThat(slaEventLogRepository.count()).isEqualTo(1);

    slaScannerJob.scan();
    assertThat(recordingNotificationPort.allMessages()).hasSize(1);
    assertThat(slaEventLogRepository.count()).isEqualTo(1);

    PullRequestState updated =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(9001L, 77L).orElseThrow();
    updated.setReadyAt(Instant.now().minusSeconds(25 * 3600));
    pullRequestStateRepository.saveAndFlush(updated);

    slaScannerJob.scan();
    assertThat(recordingNotificationPort.allMessages())
        .containsExactly(
            new NotificationMessage(9001L, 77L, SlaAction.REMIND_12H),
            new NotificationMessage(9001L, 77L, SlaAction.ESCALATE_24H));
    assertThat(slaEventLogRepository.count()).isEqualTo(2);

    updated.setFirstReviewAt(Instant.now());
    updated.setStatus(PullRequestStatus.REVIEWED);
    pullRequestStateRepository.saveAndFlush(updated);

    slaScannerJob.scan();
    assertThat(recordingNotificationPort.allMessages()).hasSize(2);
    assertThat(slaEventLogRepository.count()).isEqualTo(2);

    assertThat(counter("sla_notifications_total", "stage", "REMIND_12H")).isEqualTo(1.0);
    assertThat(counter("sla_notifications_total", "stage", "ESCALATE_24H")).isEqualTo(1.0);
    assertThat(counter("sla_scan_runs_total")).isEqualTo(4.0);
  }

  private double counter(String name, String... tags) {
    return meterRegistry.get(name).tags(tags).counter().count();
  }
}

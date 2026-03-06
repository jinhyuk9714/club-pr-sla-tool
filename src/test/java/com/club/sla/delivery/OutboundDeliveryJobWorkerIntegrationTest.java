package com.club.sla.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.github.GithubCheckDeliveryService;
import com.club.sla.github.GithubPullRequestCheckState;
import com.club.sla.metrics.SlaMetrics;
import com.club.sla.notify.DeadLetterRepository;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.NotificationPort;
import com.club.sla.sla.SchedulerLockService;
import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class OutboundDeliveryJobWorkerIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("club_sla")
          .withUsername("club_sla")
          .withPassword(UUID.randomUUID().toString());

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
  }

  @Autowired private OutboundDeliveryJobService outboundDeliveryJobService;
  @Autowired private OutboundDeliveryJobWorker outboundDeliveryJobWorker;
  @Autowired private OutboundDeliveryJobRepository outboundDeliveryJobRepository;
  @Autowired private SlaEventLogRepository slaEventLogRepository;
  @Autowired private DeadLetterRepository deadLetterRepository;

  @MockBean private NotificationPort notificationPort;
  @MockBean private GithubCheckDeliveryService githubCheckDeliveryService;
  @MockBean private SchedulerLockService schedulerLockService;
  @MockBean private SlaMetrics slaMetrics;

  @BeforeEach
  void setUp() {
    deadLetterRepository.deleteAll();
    slaEventLogRepository.deleteAll();
    outboundDeliveryJobRepository.deleteAll();
    when(schedulerLockService.tryLock("outbound-delivery-jobs")).thenReturn(true);
  }

  @Test
  void processesDiscordJobAndMarksItSucceeded() {
    outboundDeliveryJobService.enqueueDiscordNotification(
        new NotificationMessage(10L, 20L, SlaAction.REMIND_12H));

    outboundDeliveryJobWorker.processPendingJobs();

    verify(notificationPort, times(1))
        .send(new NotificationMessage(10L, 20L, SlaAction.REMIND_12H));
    assertThat(
            slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(10L, 20L, SlaAction.REMIND_12H))
        .isTrue();
    assertThat(outboundDeliveryJobRepository.findAll().get(0).getStatus())
        .isEqualTo(OutboundDeliveryJobStatus.SUCCEEDED);
  }

  @Test
  void marksGithubCheckJobDeadAndCreatesDeadLetterAfterThirdFailure() {
    outboundDeliveryJobService.enqueueGithubCheckSync(
        10L, 20L, "head-123", GithubPullRequestCheckState.BREACHED);
    OutboundDeliveryJob job = outboundDeliveryJobRepository.findAll().get(0);
    job.recordFailure("already failed once");
    job.recordFailure("already failed twice");
    outboundDeliveryJobRepository.save(job);
    doThrow(new RuntimeException("github failed"))
        .when(githubCheckDeliveryService)
        .deliver(10L, 20L, "head-123", GithubPullRequestCheckState.BREACHED);

    outboundDeliveryJobWorker.processPendingJobs();

    assertThat(outboundDeliveryJobRepository.findAll().get(0).getStatus())
        .isEqualTo(OutboundDeliveryJobStatus.DEAD);
    assertThat(deadLetterRepository.findAll()).hasSize(1);
  }
}

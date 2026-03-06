package com.club.sla.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.github.GithubPullRequestCheckState;
import com.club.sla.sla.SlaAction;
import java.time.Instant;
import java.util.UUID;
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
class OutboundDeliveryJobServiceIntegrationTest {

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
  @Autowired private OutboundDeliveryJobRepository outboundDeliveryJobRepository;

  @BeforeEach
  void setUp() {
    outboundDeliveryJobRepository.deleteAll();
  }

  @Test
  void deduplicatesDiscordNotificationJobsByRepoPrAndStage() {
    outboundDeliveryJobService.enqueueDiscordNotification(
        new com.club.sla.notify.NotificationMessage(10L, 20L, SlaAction.REMIND_12H));
    outboundDeliveryJobService.enqueueDiscordNotification(
        new com.club.sla.notify.NotificationMessage(10L, 20L, SlaAction.REMIND_12H));

    assertThat(outboundDeliveryJobRepository.findAll()).hasSize(1);
    assertThat(outboundDeliveryJobRepository.findAll().get(0).getJobType())
        .isEqualTo(OutboundDeliveryJobType.DISCORD_NOTIFICATION);
  }

  @Test
  void deduplicatesGithubCheckJobsByRepoPrHeadShaAndState() {
    outboundDeliveryJobService.enqueueGithubCheckSync(
        10L, 20L, "head-123", GithubPullRequestCheckState.AT_RISK);
    outboundDeliveryJobService.enqueueGithubCheckSync(
        10L, 20L, "head-123", GithubPullRequestCheckState.AT_RISK);

    assertThat(outboundDeliveryJobRepository.findAll()).hasSize(1);
    assertThat(outboundDeliveryJobRepository.findAll().get(0).getJobType())
        .isEqualTo(OutboundDeliveryJobType.GITHUB_CHECK_SYNC);
  }

  @Test
  void doesNotEnqueueGithubCheckJobWhenSameHeadShaAndStateAlreadySucceeded() {
    outboundDeliveryJobService.enqueueGithubCheckSync(
        10L, 20L, "head-123", GithubPullRequestCheckState.AT_RISK);
    OutboundDeliveryJob existingJob = outboundDeliveryJobRepository.findAll().get(0);
    existingJob.markSucceeded(Instant.parse("2026-03-06T00:00:00Z"));
    outboundDeliveryJobRepository.save(existingJob);

    outboundDeliveryJobService.enqueueGithubCheckSync(
        10L, 20L, "head-123", GithubPullRequestCheckState.AT_RISK);

    assertThat(outboundDeliveryJobRepository.findAll()).hasSize(1);
    assertThat(outboundDeliveryJobRepository.findAll().get(0).getStatus())
        .isEqualTo(OutboundDeliveryJobStatus.SUCCEEDED);
  }
}

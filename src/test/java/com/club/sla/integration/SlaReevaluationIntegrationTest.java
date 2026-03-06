package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.club.sla.delivery.OutboundDeliveryJobWorker;
import com.club.sla.installation.GithubInstallationAccountType;
import com.club.sla.installation.GithubInstallationRepositoryEntryUpsertCommand;
import com.club.sla.installation.GithubInstallationUpsertCommand;
import com.club.sla.installation.InstallationRegistryService;
import com.club.sla.installation.InstallationSettingsUpdateCommand;
import com.club.sla.notify.NotificationPort;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLog;
import com.club.sla.sla.SlaEventLogRepository;
import com.club.sla.sla.SlaReevaluationResultDto;
import com.club.sla.sla.SlaReevaluationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class SlaReevaluationIntegrationTest {

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

  @Autowired private PullRequestStateRepository pullRequestStateRepository;
  @Autowired private SlaEventLogRepository slaEventLogRepository;
  @Autowired private SlaReevaluationService slaReevaluationService;
  @Autowired private InstallationRegistryService installationRegistryService;
  @Autowired private OutboundDeliveryJobWorker outboundDeliveryJobWorker;

  @MockBean private NotificationPort notificationPort;

  @BeforeEach
  void setUp() {
    slaEventLogRepository.deleteAll();
    pullRequestStateRepository.deleteAll();
  }

  @Test
  void dispatchesActionAndWritesSlaLogWhenThresholdReached() {
    configureRepositoryTracking(7101L, 7001L);
    PullRequestState state = new PullRequestState(7001L, 42L, "alice");
    state.setStatus(PullRequestStatus.READY);
    state.setReadyAt(Instant.now().minus(13, ChronoUnit.HOURS));
    pullRequestStateRepository.saveAndFlush(state);

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(7001L, 42L);
    outboundDeliveryJobWorker.processPendingJobs();

    assertThat(result.actionDispatched()).isTrue();
    assertThat(result.action()).isEqualTo(SlaAction.REMIND_12H);
    assertThat(result.reason()).isEqualTo("ACTION_DISPATCHED");
    assertThat(
            slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(
                7001L, 42L, SlaAction.REMIND_12H))
        .isTrue();
    verify(notificationPort).send(any());
  }

  @Test
  void returnsAlreadySentWhenStageWasAlreadyLogged() {
    configureRepositoryTracking(7102L, 7002L);
    PullRequestState state = new PullRequestState(7002L, 43L, "alice");
    state.setStatus(PullRequestStatus.READY);
    state.setReadyAt(Instant.now().minus(13, ChronoUnit.HOURS));
    pullRequestStateRepository.saveAndFlush(state);
    slaEventLogRepository.saveAndFlush(
        new SlaEventLog(7002L, 43L, SlaAction.REMIND_12H, Instant.now().minusSeconds(60)));

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(7002L, 43L);

    assertThat(result.actionDispatched()).isFalse();
    assertThat(result.reason()).isEqualTo("ALREADY_SENT");
  }

  private void configureRepositoryTracking(Long installationId, Long repositoryId) {
    installationRegistryService.upsertInstallation(
        new GithubInstallationUpsertCommand(
            installationId,
            9000L + installationId,
            "club-org",
            GithubInstallationAccountType.ORGANIZATION,
            Instant.parse("2026-03-05T00:00:00Z")));
    installationRegistryService.syncRepositories(
        installationId,
        java.util.List.of(
            new GithubInstallationRepositoryEntryUpsertCommand(
                repositoryId, "tracked-repo", "club-org/tracked-repo")));
    installationRegistryService.saveSettings(
        installationId,
        new InstallationSettingsUpdateCommand("https://discord.example/webhook", true));
  }
}

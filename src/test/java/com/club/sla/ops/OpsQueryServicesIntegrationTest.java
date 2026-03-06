package com.club.sla.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.delivery.OutboundDeliveryJob;
import com.club.sla.delivery.OutboundDeliveryJobRepository;
import com.club.sla.delivery.OutboundDeliveryJobStatus;
import com.club.sla.delivery.OutboundDeliveryJobType;
import com.club.sla.installation.GithubInstallation;
import com.club.sla.installation.GithubInstallationAccountType;
import com.club.sla.installation.GithubInstallationRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntry;
import com.club.sla.installation.GithubInstallationRepositoryEntryRepository;
import com.club.sla.installation.GithubInstallationStatus;
import com.club.sla.installation.InstallationSettings;
import com.club.sla.installation.InstallationSettingsRepository;
import com.club.sla.notify.DeadLetterEvent;
import com.club.sla.notify.DeadLetterRepository;
import com.club.sla.sla.SlaAction;
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
class OpsQueryServicesIntegrationTest {

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

  @Autowired private OpsOverviewService opsOverviewService;
  @Autowired private OpsInstallationSummaryService opsInstallationSummaryService;
  @Autowired private OpsDeliveryQueryService opsDeliveryQueryService;
  @Autowired private GithubInstallationRepository githubInstallationRepository;

  @Autowired
  private GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository;

  @Autowired private InstallationSettingsRepository installationSettingsRepository;
  @Autowired private OutboundDeliveryJobRepository outboundDeliveryJobRepository;
  @Autowired private DeadLetterRepository deadLetterRepository;

  @BeforeEach
  void setUp() {
    deadLetterRepository.deleteAll();
    outboundDeliveryJobRepository.deleteAll();
    githubInstallationRepositoryEntryRepository.deleteAll();
    installationSettingsRepository.deleteAll();
    githubInstallationRepository.deleteAll();
  }

  @Test
  void loadsOpsOverviewCounts() {
    seedInstallations();
    seedDeliveries();
    seedDeadLetters();

    OpsOverviewView overview = opsOverviewService.loadOverview();

    assertThat(overview.activeInstallations()).isEqualTo(2);
    assertThat(overview.configuredInstallations()).isEqualTo(1);
    assertThat(overview.misconfiguredInstallations()).isEqualTo(1);
    assertThat(overview.pendingOutboundJobs()).isEqualTo(1);
    assertThat(overview.deadOutboundJobs()).isEqualTo(1);
    assertThat(overview.pendingDeadLetters()).isEqualTo(1);
  }

  @Test
  void listsInstallationSummariesWithActiveRepositoryCounts() {
    seedInstallations();

    assertThat(opsInstallationSummaryService.listInstallations())
        .extracting(
            OpsInstallationSummaryView::installationId,
            OpsInstallationSummaryView::configured,
            OpsInstallationSummaryView::activeRepositoryCount)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(7002L, false, 0L),
            org.assertj.core.groups.Tuple.tuple(7001L, true, 2L),
            org.assertj.core.groups.Tuple.tuple(7003L, true, 1L));
  }

  @Test
  void filtersRecentOutboundDeliveries() {
    seedInstallations();
    seedDeliveries();

    assertThat(
            opsDeliveryQueryService.listRecentDeliveries(
                OutboundDeliveryJobStatus.PENDING,
                OutboundDeliveryJobType.DISCORD_NOTIFICATION,
                7001L))
        .extracting(OpsDeliverySummaryView::id)
        .containsExactly(1L);
  }

  private void seedInstallations() {
    githubInstallationRepository.save(
        new GithubInstallation(
            7001L,
            1001L,
            "club-org",
            GithubInstallationAccountType.ORGANIZATION,
            GithubInstallationStatus.ACTIVE,
            Instant.parse("2026-03-05T00:00:00Z"),
            Instant.parse("2026-03-05T02:00:00Z")));
    githubInstallationRepository.save(
        new GithubInstallation(
            7002L,
            1002L,
            "beta-org",
            GithubInstallationAccountType.ORGANIZATION,
            GithubInstallationStatus.ACTIVE,
            Instant.parse("2026-03-05T00:00:00Z"),
            Instant.parse("2026-03-05T03:00:00Z")));
    githubInstallationRepository.save(
        new GithubInstallation(
            7003L,
            1003L,
            "removed-org",
            GithubInstallationAccountType.ORGANIZATION,
            GithubInstallationStatus.REMOVED,
            Instant.parse("2026-03-05T00:00:00Z"),
            Instant.parse("2026-03-05T01:00:00Z")));

    InstallationSettings configured =
        new InstallationSettings(7001L, Instant.parse("2026-03-05T02:00:00Z"));
    configured.configure("encrypted", true, Instant.parse("2026-03-05T02:00:00Z"));
    installationSettingsRepository.save(configured);

    installationSettingsRepository.save(
        new InstallationSettings(7002L, Instant.parse("2026-03-05T03:00:00Z")));

    InstallationSettings removedConfigured =
        new InstallationSettings(7003L, Instant.parse("2026-03-05T01:00:00Z"));
    removedConfigured.configure("encrypted", true, Instant.parse("2026-03-05T01:00:00Z"));
    installationSettingsRepository.save(removedConfigured);

    githubInstallationRepositoryEntryRepository.save(
        new GithubInstallationRepositoryEntry(
            7001L,
            9001L,
            "club-pr-tool",
            "club-org/club-pr-tool",
            Instant.parse("2026-03-05T02:00:00Z")));
    githubInstallationRepositoryEntryRepository.save(
        new GithubInstallationRepositoryEntry(
            7001L, 9002L, "club-web", "club-org/club-web", Instant.parse("2026-03-05T02:00:00Z")));
    GithubInstallationRepositoryEntry inactiveEntry =
        new GithubInstallationRepositoryEntry(
            7002L, 9003L, "beta-web", "beta-org/beta-web", Instant.parse("2026-03-05T03:00:00Z"));
    inactiveEntry.deactivate(Instant.parse("2026-03-05T03:30:00Z"));
    githubInstallationRepositoryEntryRepository.save(inactiveEntry);
    githubInstallationRepositoryEntryRepository.save(
        new GithubInstallationRepositoryEntry(
            7003L,
            9004L,
            "removed-web",
            "removed-org/removed-web",
            Instant.parse("2026-03-05T01:00:00Z")));
  }

  private void seedDeliveries() {
    outboundDeliveryJobRepository.save(
        new OutboundDeliveryJob(
            OutboundDeliveryJobType.DISCORD_NOTIFICATION,
            "discord:9001:17:REMIND_12H",
            7001L,
            9001L,
            17L,
            "{}",
            Instant.parse("2026-03-06T00:10:00Z")));
    OutboundDeliveryJob deadJob =
        new OutboundDeliveryJob(
            OutboundDeliveryJobType.GITHUB_CHECK_SYNC,
            "github-check:9002:18:sha:BREACHED",
            7001L,
            9002L,
            18L,
            "{}",
            Instant.parse("2026-03-06T00:11:00Z"));
    deadJob.markDead(Instant.parse("2026-03-06T00:12:00Z"), "github api down");
    outboundDeliveryJobRepository.save(deadJob);
  }

  private void seedDeadLetters() {
    deadLetterRepository.save(
        new DeadLetterEvent(
            "DISCORD_NOTIFICATION_DEAD",
            "{}",
            Instant.parse("2026-03-06T00:15:00Z"),
            9001L,
            17L,
            SlaAction.REMIND_12H));
    DeadLetterEvent replayed =
        new DeadLetterEvent(
            "GITHUB_CHECK_SYNC_DEAD",
            "{}",
            Instant.parse("2026-03-06T00:16:00Z"),
            9002L,
            18L,
            SlaAction.ESCALATE_24H);
    replayed.markReplayFailed(Instant.parse("2026-03-06T00:20:00Z"), "still failing");
    replayed.markReplaySucceeded(Instant.parse("2026-03-06T00:21:00Z"));
    deadLetterRepository.save(replayed);
  }
}

package com.club.sla.installation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
class InstallationRegistryServiceIntegrationTest {

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
    registry.add("app.security.encryption-secret", () -> "integration-test-encryption-secret");
  }

  @Autowired private InstallationRegistryService installationRegistryService;
  @Autowired private GithubInstallationRepository githubInstallationRepository;

  @Autowired
  private GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository;

  @Autowired private InstallationSettingsRepository installationSettingsRepository;
  @Autowired private DiscordWebhookCipher discordWebhookCipher;

  @BeforeEach
  void setUp() {
    installationSettingsRepository.deleteAll();
    githubInstallationRepositoryEntryRepository.deleteAll();
    githubInstallationRepository.deleteAll();
  }

  @Test
  void upsertsInstallationAndSeedsDefaultSettings() {
    Instant now = Instant.parse("2026-03-06T00:00:00Z");

    installationRegistryService.upsertInstallation(
        new GithubInstallationUpsertCommand(
            7001L, 991L, "club-org", GithubInstallationAccountType.ORGANIZATION, now));

    GithubInstallation installation = githubInstallationRepository.findById(7001L).orElseThrow();
    assertThat(installation.getStatus()).isEqualTo(GithubInstallationStatus.ACTIVE);
    assertThat(installation.getAccountLogin()).isEqualTo("club-org");

    InstallationSettings settings = installationSettingsRepository.findById(7001L).orElseThrow();
    assertThat(settings.isConfigured()).isFalse();
    assertThat(settings.getReminderHours()).isEqualTo(12);
    assertThat(settings.getEscalationHours()).isEqualTo(24);
    assertThat(settings.isFallbackEnabled()).isFalse();
  }

  @Test
  void syncsRepositoriesAndMarksMissingRepositoriesInactive() {
    Instant now = Instant.parse("2026-03-06T00:00:00Z");
    installationRegistryService.upsertInstallation(
        new GithubInstallationUpsertCommand(
            7002L, 992L, "club-org", GithubInstallationAccountType.ORGANIZATION, now));

    installationRegistryService.syncRepositories(
        7002L,
        List.of(
            new GithubInstallationRepositoryEntryUpsertCommand(
                9001L, "club-pr-tool", "club-org/club-pr-tool"),
            new GithubInstallationRepositoryEntryUpsertCommand(
                9002L, "club-web", "club-org/club-web")));

    installationRegistryService.syncRepositories(
        7002L,
        List.of(
            new GithubInstallationRepositoryEntryUpsertCommand(
                9002L, "club-web", "club-org/club-web")));

    GithubInstallationRepositoryEntry inactiveRepository =
        githubInstallationRepositoryEntryRepository.findByRepositoryId(9001L).orElseThrow();
    GithubInstallationRepositoryEntry activeRepository =
        githubInstallationRepositoryEntryRepository.findByRepositoryId(9002L).orElseThrow();

    assertThat(inactiveRepository.isActive()).isFalse();
    assertThat(activeRepository.isActive()).isTrue();
  }

  @Test
  void storesEncryptedWebhookAfterSettingsAreSaved() {
    Instant now = Instant.parse("2026-03-06T00:00:00Z");
    installationRegistryService.upsertInstallation(
        new GithubInstallationUpsertCommand(
            7003L, 993L, "club-org", GithubInstallationAccountType.ORGANIZATION, now));

    installationRegistryService.saveSettings(
        7003L, new InstallationSettingsUpdateCommand("https://discord.example/webhook", true));

    InstallationSettings settings = installationSettingsRepository.findById(7003L).orElseThrow();
    assertThat(settings.isConfigured()).isTrue();
    assertThat(settings.getEncryptedDiscordWebhook())
        .isNotEqualTo("https://discord.example/webhook");
    assertThat(discordWebhookCipher.decrypt(settings.getEncryptedDiscordWebhook()))
        .isEqualTo("https://discord.example/webhook");
  }
}

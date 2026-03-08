package com.club.sla.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.club.sla.github.GithubAppClient;
import com.club.sla.github.GithubAuthenticatedUser;
import com.club.sla.github.GithubInstallationMetadata;
import com.club.sla.github.GithubRepositoryMetadata;
import com.club.sla.installation.DiscordWebhookCipher;
import com.club.sla.installation.GithubInstallationAccountType;
import com.club.sla.installation.GithubInstallationRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntryRepository;
import com.club.sla.installation.InstallationRegistryService;
import com.club.sla.installation.InstallationSettingsRepository;
import com.club.sla.security.BetaAccessPolicy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultInstallationOnboardingServiceTest {

  @Mock private GithubAppClient githubAppClient;
  @Mock private InstallationRegistryService installationRegistryService;
  @Mock private GithubInstallationRepository githubInstallationRepository;

  @Mock
  private GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository;

  @Mock private InstallationSettingsRepository installationSettingsRepository;
  @Mock private DiscordWebhookValidationService discordWebhookValidationService;
  @Mock private DiscordWebhookCipher discordWebhookCipher;
  @Mock private BetaAccessPolicy betaAccessPolicy;

  @Test
  void listsAccessibleInstallationsForAllowlistedUser() {
    DefaultInstallationOnboardingService service =
        new DefaultInstallationOnboardingService(
            githubAppClient,
            installationRegistryService,
            githubInstallationRepository,
            githubInstallationRepositoryEntryRepository,
            installationSettingsRepository,
            discordWebhookValidationService,
            discordWebhookCipher,
            betaAccessPolicy);
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    GithubInstallationMetadata firstInstallation =
        new GithubInstallationMetadata(
            7001L, 991L, "club-org", GithubInstallationAccountType.ORGANIZATION, Instant.now());
    GithubInstallationMetadata secondInstallation =
        new GithubInstallationMetadata(
            7002L, 992L, "blocked-org", GithubInstallationAccountType.ORGANIZATION, Instant.now());
    given(githubAppClient.listUserInstallations("user-token"))
        .willReturn(List.of(firstInstallation, secondInstallation));
    given(betaAccessPolicy.isAllowed("alice", "club-org")).willReturn(true);
    given(betaAccessPolicy.isAllowed("alice", "blocked-org")).willReturn(false);
    given(githubAppClient.listInstallationRepositories(7001L))
        .willReturn(
            List.of(new GithubRepositoryMetadata(9001L, "club-pr-tool", "club-org/club-pr-tool")));

    List<InstallationOnboardingView> result =
        service.listAccessibleInstallations(authenticatedUser);

    assertThat(result)
        .containsExactly(
            new InstallationOnboardingView(
                7001L, "club-org", List.of("club-org/club-pr-tool"), false));
    verify(installationRegistryService).upsertInstallation(any());
    verify(installationRegistryService).syncRepositories(any(), any());
  }

  @Test
  void syncsInstallationAndBuildsView() {
    DefaultInstallationOnboardingService service =
        new DefaultInstallationOnboardingService(
            githubAppClient,
            installationRegistryService,
            githubInstallationRepository,
            githubInstallationRepositoryEntryRepository,
            installationSettingsRepository,
            discordWebhookValidationService,
            discordWebhookCipher,
            betaAccessPolicy);
    GithubInstallationMetadata installationMetadata =
        new GithubInstallationMetadata(
            7001L, 991L, "club-org", GithubInstallationAccountType.ORGANIZATION, Instant.now());
    given(githubAppClient.fetchInstallation(7001L)).willReturn(installationMetadata);
    given(githubAppClient.listInstallationRepositories(7001L))
        .willReturn(
            List.of(
                new GithubRepositoryMetadata(9001L, "club-pr-tool", "club-org/club-pr-tool"),
                new GithubRepositoryMetadata(9002L, "club-web", "club-org/club-web")));

    InstallationOnboardingView installationOnboardingView = service.loadInstallationView(7001L);

    verify(installationRegistryService).upsertInstallation(any());
    verify(installationRegistryService).syncRepositories(any(), any());
    assertThat(installationOnboardingView.installationId()).isEqualTo(7001L);
    assertThat(installationOnboardingView.accountLogin()).isEqualTo("club-org");
    assertThat(installationOnboardingView.repositoryFullNames())
        .containsExactly("club-org/club-pr-tool", "club-org/club-web");
    assertThat(installationOnboardingView.configured()).isFalse();
  }

  @Test
  void throwsValidationErrorWhenDiscordWebhookValidationFails() {
    DefaultInstallationOnboardingService service =
        new DefaultInstallationOnboardingService(
            githubAppClient,
            installationRegistryService,
            githubInstallationRepository,
            githubInstallationRepositoryEntryRepository,
            installationSettingsRepository,
            discordWebhookValidationService,
            discordWebhookCipher,
            betaAccessPolicy);
    org.mockito.Mockito.doThrow(new RuntimeException("boom"))
        .when(discordWebhookValidationService)
        .validate("https://discord.example/webhook");

    assertThatThrownBy(() -> service.saveSettings(7001L, "https://discord.example/webhook"))
        .isInstanceOf(InvalidDiscordWebhookException.class)
        .hasMessageContaining("Discord webhook validation failed");
  }

  @Test
  void validatesWebhookAndStoresSettings() {
    DefaultInstallationOnboardingService service =
        new DefaultInstallationOnboardingService(
            githubAppClient,
            installationRegistryService,
            githubInstallationRepository,
            githubInstallationRepositoryEntryRepository,
            installationSettingsRepository,
            discordWebhookValidationService,
            discordWebhookCipher,
            betaAccessPolicy);

    service.saveSettings(7001L, "https://discord.example/webhook");

    verify(discordWebhookValidationService).validate("https://discord.example/webhook");
    ArgumentCaptor<String> webhookCaptor = ArgumentCaptor.forClass(String.class);
    verify(installationRegistryService).saveSettings(org.mockito.Mockito.eq(7001L), any());
  }

  @Test
  void delegatesInstallationAccessCheckToGithubClient() {
    DefaultInstallationOnboardingService service =
        new DefaultInstallationOnboardingService(
            githubAppClient,
            installationRegistryService,
            githubInstallationRepository,
            githubInstallationRepositoryEntryRepository,
            installationSettingsRepository,
            discordWebhookValidationService,
            discordWebhookCipher,
            betaAccessPolicy);
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubAppClient.userCanAccessInstallation("user-token", 7001L)).willReturn(true);
    given(githubAppClient.fetchInstallation(7001L))
        .willReturn(
            new GithubInstallationMetadata(
                7001L,
                991L,
                "club-org",
                GithubInstallationAccountType.ORGANIZATION,
                Instant.parse("2026-03-06T00:00:00Z")));
    given(betaAccessPolicy.isAllowed("alice", "club-org")).willReturn(true);

    boolean result = service.userCanAccessInstallation(authenticatedUser, 7001L);

    assertThat(result).isTrue();
  }

  @Test
  void deniesInstallationAccessWhenUserIsNotAllowlisted() {
    DefaultInstallationOnboardingService service =
        new DefaultInstallationOnboardingService(
            githubAppClient,
            installationRegistryService,
            githubInstallationRepository,
            githubInstallationRepositoryEntryRepository,
            installationSettingsRepository,
            discordWebhookValidationService,
            discordWebhookCipher,
            betaAccessPolicy);
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubAppClient.userCanAccessInstallation("user-token", 7001L)).willReturn(true);
    given(githubAppClient.fetchInstallation(7001L))
        .willReturn(
            new GithubInstallationMetadata(
                7001L,
                991L,
                "club-org",
                GithubInstallationAccountType.ORGANIZATION,
                Instant.parse("2026-03-06T00:00:00Z")));
    given(betaAccessPolicy.isAllowed("alice", "club-org")).willReturn(false);

    boolean result = service.userCanAccessInstallation(authenticatedUser, 7001L);

    assertThat(result).isFalse();
  }
}

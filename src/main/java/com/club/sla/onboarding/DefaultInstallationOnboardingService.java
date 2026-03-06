package com.club.sla.onboarding;

import com.club.sla.github.GithubAppClient;
import com.club.sla.github.GithubAuthenticatedUser;
import com.club.sla.github.GithubInstallationMetadata;
import com.club.sla.github.GithubRepositoryMetadata;
import com.club.sla.installation.GithubInstallationRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntryRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntryUpsertCommand;
import com.club.sla.installation.GithubInstallationUpsertCommand;
import com.club.sla.installation.InstallationRegistryService;
import com.club.sla.installation.InstallationSettings;
import com.club.sla.installation.InstallationSettingsRepository;
import com.club.sla.installation.InstallationSettingsUpdateCommand;
import com.club.sla.security.BetaAccessPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultInstallationOnboardingService implements InstallationOnboardingService {

  private final GithubAppClient githubAppClient;
  private final InstallationRegistryService installationRegistryService;
  private final GithubInstallationRepository githubInstallationRepository;
  private final GithubInstallationRepositoryEntryRepository
      githubInstallationRepositoryEntryRepository;
  private final InstallationSettingsRepository installationSettingsRepository;
  private final DiscordWebhookValidationService discordWebhookValidationService;
  private final BetaAccessPolicy betaAccessPolicy;

  public DefaultInstallationOnboardingService(
      GithubAppClient githubAppClient,
      InstallationRegistryService installationRegistryService,
      GithubInstallationRepository githubInstallationRepository,
      GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository,
      InstallationSettingsRepository installationSettingsRepository,
      DiscordWebhookValidationService discordWebhookValidationService,
      com.club.sla.installation.DiscordWebhookCipher discordWebhookCipher,
      BetaAccessPolicy betaAccessPolicy) {
    this.githubAppClient = githubAppClient;
    this.installationRegistryService = installationRegistryService;
    this.githubInstallationRepository = githubInstallationRepository;
    this.githubInstallationRepositoryEntryRepository = githubInstallationRepositoryEntryRepository;
    this.installationSettingsRepository = installationSettingsRepository;
    this.discordWebhookValidationService = discordWebhookValidationService;
    this.betaAccessPolicy = betaAccessPolicy;
  }

  @Override
  public boolean userCanAccessInstallation(
      GithubAuthenticatedUser authenticatedUser, Long installationId) {
    if (!githubAppClient.userCanAccessInstallation(
        authenticatedUser.accessToken(), installationId)) {
      return false;
    }
    GithubInstallationMetadata installationMetadata =
        githubAppClient.fetchInstallation(installationId);
    return betaAccessPolicy.isAllowed(
        authenticatedUser.login(), installationMetadata.accountLogin());
  }

  @Override
  public InstallationOnboardingView loadInstallationView(Long installationId) {
    GithubInstallationMetadata installationMetadata =
        githubAppClient.fetchInstallation(installationId);
    List<GithubRepositoryMetadata> repositoryMetadata =
        githubAppClient.listInstallationRepositories(installationId);

    installationRegistryService.upsertInstallation(
        new GithubInstallationUpsertCommand(
            installationMetadata.installationId(),
            installationMetadata.accountId(),
            installationMetadata.accountLogin(),
            installationMetadata.accountType(),
            installationMetadata.installedAt()));
    installationRegistryService.syncRepositories(
        installationId,
        repositoryMetadata.stream()
            .map(
                repository ->
                    new GithubInstallationRepositoryEntryUpsertCommand(
                        repository.repositoryId(),
                        repository.repositoryName(),
                        repository.repositoryFullName()))
            .toList());

    Optional<InstallationSettings> maybeSettings =
        Optional.ofNullable(installationSettingsRepository.findById(installationId))
            .orElse(Optional.empty());
    return new InstallationOnboardingView(
        installationId,
        installationMetadata.accountLogin(),
        repositoryMetadata.stream().map(GithubRepositoryMetadata::repositoryFullName).toList(),
        maybeSettings.map(InstallationSettings::isConfigured).orElse(false));
  }

  @Override
  public void saveSettings(Long installationId, String discordWebhookUrl) {
    try {
      discordWebhookValidationService.validate(discordWebhookUrl);
    } catch (RuntimeException ex) {
      throw new InvalidDiscordWebhookException("Discord webhook validation failed");
    }
    installationRegistryService.saveSettings(
        installationId, new InstallationSettingsUpdateCommand(discordWebhookUrl, true));
  }
}

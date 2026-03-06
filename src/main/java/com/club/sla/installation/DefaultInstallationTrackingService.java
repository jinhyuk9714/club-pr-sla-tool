package com.club.sla.installation;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultInstallationTrackingService implements InstallationTrackingService {

  private final GithubInstallationRepository githubInstallationRepository;
  private final GithubInstallationRepositoryEntryRepository
      githubInstallationRepositoryEntryRepository;
  private final InstallationSettingsRepository installationSettingsRepository;
  private final DiscordWebhookCipher discordWebhookCipher;

  public DefaultInstallationTrackingService(
      GithubInstallationRepository githubInstallationRepository,
      GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository,
      InstallationSettingsRepository installationSettingsRepository,
      DiscordWebhookCipher discordWebhookCipher) {
    this.githubInstallationRepository = githubInstallationRepository;
    this.githubInstallationRepositoryEntryRepository = githubInstallationRepositoryEntryRepository;
    this.installationSettingsRepository = installationSettingsRepository;
    this.discordWebhookCipher = discordWebhookCipher;
  }

  @Override
  public boolean isRepositoryActive(Long repositoryId) {
    return githubInstallationRepositoryEntryRepository
        .findByRepositoryId(repositoryId)
        .filter(GithubInstallationRepositoryEntry::isActive)
        .flatMap(entry -> githubInstallationRepository.findById(entry.getInstallationId()))
        .map(installation -> installation.getStatus() == GithubInstallationStatus.ACTIVE)
        .orElse(false);
  }

  @Override
  public boolean isRepositoryConfigured(Long repositoryId) {
    return githubInstallationRepositoryEntryRepository
        .findByRepositoryId(repositoryId)
        .filter(GithubInstallationRepositoryEntry::isActive)
        .flatMap(
            entry ->
                installationSettingsRepository
                    .findById(entry.getInstallationId())
                    .filter(InstallationSettings::isConfigured))
        .isPresent();
  }

  @Override
  public Optional<String> findConfiguredDiscordWebhook(Long repositoryId) {
    return githubInstallationRepositoryEntryRepository
        .findByRepositoryId(repositoryId)
        .filter(GithubInstallationRepositoryEntry::isActive)
        .flatMap(entry -> installationSettingsRepository.findById(entry.getInstallationId()))
        .filter(InstallationSettings::isConfigured)
        .map(InstallationSettings::getEncryptedDiscordWebhook)
        .map(discordWebhookCipher::decrypt);
  }

  @Override
  public Optional<String> findRepositoryFullName(Long repositoryId) {
    return githubInstallationRepositoryEntryRepository
        .findByRepositoryId(repositoryId)
        .map(GithubInstallationRepositoryEntry::getRepositoryFullName);
  }

  @Override
  public Optional<Long> findInstallationId(Long repositoryId) {
    return githubInstallationRepositoryEntryRepository
        .findByRepositoryId(repositoryId)
        .map(GithubInstallationRepositoryEntry::getInstallationId);
  }
}

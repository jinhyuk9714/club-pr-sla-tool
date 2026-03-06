package com.club.sla.installation;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstallationRegistryService {

  private final GithubInstallationRepository githubInstallationRepository;
  private final GithubInstallationRepositoryEntryRepository
      githubInstallationRepositoryEntryRepository;
  private final InstallationSettingsRepository installationSettingsRepository;
  private final DiscordWebhookCipher discordWebhookCipher;
  private final Clock clock;

  @Autowired
  public InstallationRegistryService(
      GithubInstallationRepository githubInstallationRepository,
      GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository,
      InstallationSettingsRepository installationSettingsRepository,
      DiscordWebhookCipher discordWebhookCipher) {
    this(
        githubInstallationRepository,
        githubInstallationRepositoryEntryRepository,
        installationSettingsRepository,
        discordWebhookCipher,
        Clock.systemUTC());
  }

  InstallationRegistryService(
      GithubInstallationRepository githubInstallationRepository,
      GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository,
      InstallationSettingsRepository installationSettingsRepository,
      DiscordWebhookCipher discordWebhookCipher,
      Clock clock) {
    this.githubInstallationRepository = githubInstallationRepository;
    this.githubInstallationRepositoryEntryRepository = githubInstallationRepositoryEntryRepository;
    this.installationSettingsRepository = installationSettingsRepository;
    this.discordWebhookCipher = discordWebhookCipher;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Transactional
  public GithubInstallation upsertInstallation(GithubInstallationUpsertCommand command) {
    Instant now = command.installedAt() != null ? command.installedAt() : Instant.now(clock);
    GithubInstallation installation =
        githubInstallationRepository
            .findById(command.installationId())
            .orElseGet(
                () ->
                    new GithubInstallation(
                        command.installationId(),
                        command.accountId(),
                        command.accountLogin(),
                        command.accountType(),
                        GithubInstallationStatus.ACTIVE,
                        now,
                        now));
    installation.activate(
        command.accountId(),
        command.accountLogin(),
        command.accountType(),
        now,
        Instant.now(clock));
    GithubInstallation saved = githubInstallationRepository.save(installation);
    installationSettingsRepository
        .findById(command.installationId())
        .orElseGet(
            () ->
                installationSettingsRepository.save(
                    new InstallationSettings(command.installationId(), Instant.now(clock))));
    return saved;
  }

  @Transactional
  public void syncRepositories(
      Long installationId, List<GithubInstallationRepositoryEntryUpsertCommand> commands) {
    Instant now = Instant.now(clock);
    Set<Long> activeRepositoryIds = new HashSet<>();
    for (GithubInstallationRepositoryEntryUpsertCommand command : commands) {
      activeRepositoryIds.add(command.repositoryId());
      GithubInstallationRepositoryEntry entry =
          githubInstallationRepositoryEntryRepository
              .findByRepositoryId(command.repositoryId())
              .orElseGet(
                  () ->
                      new GithubInstallationRepositoryEntry(
                          installationId,
                          command.repositoryId(),
                          command.repositoryName(),
                          command.repositoryFullName(),
                          now));
      entry.update(command.repositoryName(), command.repositoryFullName(), now);
      githubInstallationRepositoryEntryRepository.save(entry);
    }

    List<GithubInstallationRepositoryEntry> existingEntries =
        githubInstallationRepositoryEntryRepository.findByInstallationId(installationId);
    for (GithubInstallationRepositoryEntry existingEntry : existingEntries) {
      if (!activeRepositoryIds.contains(existingEntry.getRepositoryId())) {
        existingEntry.deactivate(now);
        githubInstallationRepositoryEntryRepository.save(existingEntry);
      }
    }
  }

  @Transactional
  public void upsertRepositories(
      Long installationId, List<GithubInstallationRepositoryEntryUpsertCommand> commands) {
    Instant now = Instant.now(clock);
    for (GithubInstallationRepositoryEntryUpsertCommand command : commands) {
      GithubInstallationRepositoryEntry entry =
          githubInstallationRepositoryEntryRepository
              .findByRepositoryId(command.repositoryId())
              .orElseGet(
                  () ->
                      new GithubInstallationRepositoryEntry(
                          installationId,
                          command.repositoryId(),
                          command.repositoryName(),
                          command.repositoryFullName(),
                          now));
      entry.update(command.repositoryName(), command.repositoryFullName(), now);
      githubInstallationRepositoryEntryRepository.save(entry);
    }
  }

  @Transactional
  public void deactivateRepositories(Long installationId, List<Long> repositoryIds) {
    Instant now = Instant.now(clock);
    List<GithubInstallationRepositoryEntry> existingEntries =
        githubInstallationRepositoryEntryRepository.findByInstallationId(installationId);
    for (GithubInstallationRepositoryEntry existingEntry : existingEntries) {
      if (repositoryIds.contains(existingEntry.getRepositoryId())) {
        existingEntry.deactivate(now);
        githubInstallationRepositoryEntryRepository.save(existingEntry);
      }
    }
  }

  @Transactional
  public void markInstallationStatus(Long installationId, GithubInstallationStatus status) {
    githubInstallationRepository
        .findById(installationId)
        .ifPresent(
            installation -> {
              installation.setStatus(status, Instant.now(clock));
              githubInstallationRepository.save(installation);
            });
  }

  @Transactional
  public InstallationSettings saveSettings(
      Long installationId, InstallationSettingsUpdateCommand command) {
    InstallationSettings settings =
        installationSettingsRepository
            .findById(installationId)
            .orElseGet(() -> new InstallationSettings(installationId, Instant.now(clock)));
    settings.configure(
        discordWebhookCipher.encrypt(command.discordWebhookUrl()),
        command.configured(),
        Instant.now(clock));
    return installationSettingsRepository.save(settings);
  }
}

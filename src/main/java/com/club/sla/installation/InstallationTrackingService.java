package com.club.sla.installation;

import java.util.Optional;

public interface InstallationTrackingService {

  boolean isRepositoryActive(Long repositoryId);

  boolean isRepositoryConfigured(Long repositoryId);

  Optional<String> findConfiguredDiscordWebhook(Long repositoryId);

  Optional<String> findRepositoryFullName(Long repositoryId);

  Optional<Long> findInstallationId(Long repositoryId);
}

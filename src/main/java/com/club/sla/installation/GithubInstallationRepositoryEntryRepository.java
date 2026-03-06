package com.club.sla.installation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubInstallationRepositoryEntryRepository
    extends JpaRepository<GithubInstallationRepositoryEntry, Long> {

  Optional<GithubInstallationRepositoryEntry> findByRepositoryId(Long repositoryId);

  List<GithubInstallationRepositoryEntry> findByInstallationId(Long installationId);
}

package com.club.sla.installation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "github_installation_repository_entries",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_github_installation_repository_entries_installation_repository",
          columnNames = {"installation_id", "repository_id"}),
      @UniqueConstraint(
          name = "uk_github_installation_repository_entries_repository",
          columnNames = {"repository_id"})
    })
public class GithubInstallationRepositoryEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "installation_id", nullable = false)
  private Long installationId;

  @Column(name = "repository_id", nullable = false)
  private Long repositoryId;

  @Column(name = "repository_name", nullable = false)
  private String repositoryName;

  @Column(name = "repository_full_name", nullable = false)
  private String repositoryFullName;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected GithubInstallationRepositoryEntry() {}

  public GithubInstallationRepositoryEntry(
      Long installationId,
      Long repositoryId,
      String repositoryName,
      String repositoryFullName,
      Instant updatedAt) {
    this.installationId = installationId;
    this.repositoryId = repositoryId;
    this.repositoryName = repositoryName;
    this.repositoryFullName = repositoryFullName;
    this.active = true;
    this.updatedAt = updatedAt;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRepositoryFullName() {
    return repositoryFullName;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(String repositoryName, String repositoryFullName, Instant updatedAt) {
    this.repositoryName = repositoryName;
    this.repositoryFullName = repositoryFullName;
    this.active = true;
    this.updatedAt = updatedAt;
  }

  public void deactivate(Instant updatedAt) {
    this.active = false;
    this.updatedAt = updatedAt;
  }
}

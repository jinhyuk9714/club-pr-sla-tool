package com.club.sla.installation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "github_installations")
public class GithubInstallation {

  @Id
  @Column(name = "installation_id", nullable = false)
  private Long installationId;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "account_login", nullable = false)
  private String accountLogin;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false)
  private GithubInstallationAccountType accountType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private GithubInstallationStatus status;

  @Column(name = "installed_at", nullable = false)
  private Instant installedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected GithubInstallation() {}

  public GithubInstallation(
      Long installationId,
      Long accountId,
      String accountLogin,
      GithubInstallationAccountType accountType,
      GithubInstallationStatus status,
      Instant installedAt,
      Instant updatedAt) {
    this.installationId = installationId;
    this.accountId = accountId;
    this.accountLogin = accountLogin;
    this.accountType = accountType;
    this.status = status;
    this.installedAt = installedAt;
    this.updatedAt = updatedAt;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getAccountLogin() {
    return accountLogin;
  }

  public GithubInstallationAccountType getAccountType() {
    return accountType;
  }

  public GithubInstallationStatus getStatus() {
    return status;
  }

  public Instant getInstalledAt() {
    return installedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void activate(
      Long accountId,
      String accountLogin,
      GithubInstallationAccountType accountType,
      Instant installedAt,
      Instant updatedAt) {
    this.accountId = accountId;
    this.accountLogin = accountLogin;
    this.accountType = accountType;
    this.status = GithubInstallationStatus.ACTIVE;
    this.installedAt = installedAt;
    this.updatedAt = updatedAt;
  }

  public void setStatus(GithubInstallationStatus status, Instant updatedAt) {
    this.status = status;
    this.updatedAt = updatedAt;
  }
}

package com.club.sla.installation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "installation_settings")
public class InstallationSettings {

  @Id
  @Column(name = "installation_id", nullable = false)
  private Long installationId;

  @Column(name = "encrypted_discord_webhook")
  private String encryptedDiscordWebhook;

  @Column(name = "reminder_hours", nullable = false)
  private int reminderHours;

  @Column(name = "escalation_hours", nullable = false)
  private int escalationHours;

  @Column(name = "fallback_enabled", nullable = false)
  private boolean fallbackEnabled;

  @Column(name = "configured", nullable = false)
  private boolean configured;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected InstallationSettings() {}

  public InstallationSettings(Long installationId, Instant updatedAt) {
    this.installationId = installationId;
    this.reminderHours = 12;
    this.escalationHours = 24;
    this.fallbackEnabled = false;
    this.configured = false;
    this.updatedAt = updatedAt;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public String getEncryptedDiscordWebhook() {
    return encryptedDiscordWebhook;
  }

  public int getReminderHours() {
    return reminderHours;
  }

  public int getEscalationHours() {
    return escalationHours;
  }

  public boolean isFallbackEnabled() {
    return fallbackEnabled;
  }

  public boolean isConfigured() {
    return configured;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void configure(String encryptedDiscordWebhook, boolean configured, Instant updatedAt) {
    this.encryptedDiscordWebhook = encryptedDiscordWebhook;
    this.configured = configured;
    this.updatedAt = updatedAt;
  }
}

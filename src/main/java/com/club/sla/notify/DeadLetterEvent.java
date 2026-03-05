package com.club.sla.notify;

import com.club.sla.sla.SlaAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "dead_letters")
public class DeadLetterEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reason", nullable = false)
  private String reason;

  @Column(name = "payload", nullable = false, columnDefinition = "text")
  private String payload;

  @Column(name = "repo_id")
  private Long repoId;

  @Column(name = "pr_number")
  private Long prNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "stage")
  private SlaAction stage;

  @Enumerated(EnumType.STRING)
  @Column(name = "replay_status", nullable = false)
  private DeadLetterReplayStatus replayStatus;

  @Column(name = "replay_attempts", nullable = false)
  private int replayAttempts;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  @Column(name = "replayed_at")
  private Instant replayedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DeadLetterEvent() {}

  public DeadLetterEvent(String reason, String payload, Instant createdAt) {
    this(reason, payload, createdAt, null, null, null);
  }

  public DeadLetterEvent(
      String reason,
      String payload,
      Instant createdAt,
      Long repoId,
      Long prNumber,
      SlaAction stage) {
    this.reason = reason;
    this.payload = payload;
    this.repoId = repoId;
    this.prNumber = prNumber;
    this.stage = stage;
    this.replayStatus = DeadLetterReplayStatus.PENDING;
    this.replayAttempts = 0;
    this.lastError = null;
    this.replayedAt = null;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getReason() {
    return reason;
  }

  public String getPayload() {
    return payload;
  }

  public Long getRepoId() {
    return repoId;
  }

  public Long getPrNumber() {
    return prNumber;
  }

  public SlaAction getStage() {
    return stage;
  }

  public DeadLetterReplayStatus getReplayStatus() {
    return replayStatus;
  }

  public int getReplayAttempts() {
    return replayAttempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getReplayedAt() {
    return replayedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markReplaySucceeded(Instant now) {
    this.replayStatus = DeadLetterReplayStatus.REPLAYED;
    this.replayAttempts += 1;
    this.lastError = null;
    this.replayedAt = now;
    this.updatedAt = now;
  }

  public void markReplayFailed(Instant now, String errorMessage) {
    this.replayStatus = DeadLetterReplayStatus.FAILED;
    this.replayAttempts += 1;
    this.lastError = errorMessage;
    this.updatedAt = now;
  }
}

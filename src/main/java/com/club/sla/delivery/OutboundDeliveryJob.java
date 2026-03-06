package com.club.sla.delivery;

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
@Table(name = "outbound_delivery_jobs")
public class OutboundDeliveryJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_type", nullable = false)
  private OutboundDeliveryJobType jobType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OutboundDeliveryJobStatus status;

  @Column(name = "unique_key", nullable = false)
  private String uniqueKey;

  @Column(name = "installation_id")
  private Long installationId;

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "pr_number")
  private Long prNumber;

  @Column(name = "payload_json", nullable = false, columnDefinition = "text")
  private String payloadJson;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected OutboundDeliveryJob() {}

  public OutboundDeliveryJob(
      OutboundDeliveryJobType jobType,
      String uniqueKey,
      Long installationId,
      Long repositoryId,
      Long prNumber,
      String payloadJson,
      Instant now) {
    this.jobType = jobType;
    this.status = OutboundDeliveryJobStatus.PENDING;
    this.uniqueKey = uniqueKey;
    this.installationId = installationId;
    this.repositoryId = repositoryId;
    this.prNumber = prNumber;
    this.payloadJson = payloadJson;
    this.attemptCount = 0;
    this.nextAttemptAt = now;
    this.lastError = null;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public OutboundDeliveryJobType getJobType() {
    return jobType;
  }

  public OutboundDeliveryJobStatus getStatus() {
    return status;
  }

  public String getUniqueKey() {
    return uniqueKey;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public Long getPrNumber() {
    return prNumber;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markProcessing(Instant now) {
    this.status = OutboundDeliveryJobStatus.PROCESSING;
    this.updatedAt = now;
  }

  public void markSucceeded(Instant now) {
    this.status = OutboundDeliveryJobStatus.SUCCEEDED;
    this.updatedAt = now;
  }

  public void scheduleRetry(Instant now, String errorMessage, Instant nextAttemptAt) {
    this.status = OutboundDeliveryJobStatus.PENDING;
    this.attemptCount += 1;
    this.lastError = errorMessage;
    this.nextAttemptAt = nextAttemptAt;
    this.updatedAt = now;
  }

  public void markDead(Instant now, String errorMessage) {
    this.status = OutboundDeliveryJobStatus.DEAD;
    this.attemptCount += 1;
    this.lastError = errorMessage;
    this.updatedAt = now;
  }

  public void recordFailure(String errorMessage) {
    this.attemptCount += 1;
    this.lastError = errorMessage;
    this.updatedAt = Instant.now();
  }
}

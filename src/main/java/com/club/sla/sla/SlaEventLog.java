package com.club.sla.sla;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "sla_event_logs",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_sla_event_logs_repo_pr_stage",
          columnNames = {"repo_id", "pr_number", "stage"})
    })
public class SlaEventLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "repo_id", nullable = false)
  private Long repoId;

  @Column(name = "pr_number", nullable = false)
  private Long prNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "stage", nullable = false)
  private SlaAction stage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected SlaEventLog() {}

  public SlaEventLog(Long repoId, Long prNumber, SlaAction stage, Instant createdAt) {
    this.repoId = repoId;
    this.prNumber = prNumber;
    this.stage = stage;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
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

  public Instant getCreatedAt() {
    return createdAt;
  }
}

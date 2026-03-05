package com.club.sla.pr;

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
    name = "pull_request_states",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_pull_request_states_repository_pr",
          columnNames = {"repository_id", "pr_number"})
    })
public class PullRequestState {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "repository_id", nullable = false)
  private Long repositoryId;

  @Column(name = "pr_number", nullable = false)
  private Long prNumber;

  @Column(name = "author_login")
  private String authorLogin;

  @Column(name = "first_reviewer_login")
  private String firstReviewerLogin;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PullRequestStatus status;

  @Column(name = "ready_at")
  private Instant readyAt;

  @Column(name = "first_review_at")
  private Instant firstReviewAt;

  protected PullRequestState() {}

  public PullRequestState(Long repositoryId, Long prNumber, String authorLogin) {
    this.repositoryId = repositoryId;
    this.prNumber = prNumber;
    this.authorLogin = authorLogin;
    this.status = PullRequestStatus.DRAFT;
  }

  public Long getId() {
    return id;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public Long getPrNumber() {
    return prNumber;
  }

  public String getAuthorLogin() {
    return authorLogin;
  }

  public void setAuthorLogin(String authorLogin) {
    this.authorLogin = authorLogin;
  }

  public String getFirstReviewerLogin() {
    return firstReviewerLogin;
  }

  public void setFirstReviewerLogin(String firstReviewerLogin) {
    this.firstReviewerLogin = firstReviewerLogin;
  }

  public PullRequestStatus getStatus() {
    return status;
  }

  public void setStatus(PullRequestStatus status) {
    this.status = status;
  }

  public Instant getReadyAt() {
    return readyAt;
  }

  public void setReadyAt(Instant readyAt) {
    this.readyAt = readyAt;
  }

  public Instant getFirstReviewAt() {
    return firstReviewAt;
  }

  public void setFirstReviewAt(Instant firstReviewAt) {
    this.firstReviewAt = firstReviewAt;
  }
}

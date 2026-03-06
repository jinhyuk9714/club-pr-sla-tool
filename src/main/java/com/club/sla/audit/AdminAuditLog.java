package com.club.sla.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "operation", nullable = false, length = 64)
  private String operation;

  @Column(name = "request_path", nullable = false, length = 255)
  private String requestPath;

  @Column(name = "http_method", nullable = false, length = 8)
  private String httpMethod;

  @Column(name = "http_status", nullable = false)
  private int httpStatus;

  @Column(name = "outcome", nullable = false, length = 32)
  private String outcome;

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "pr_number")
  private Long prNumber;

  @Column(name = "dead_letter_id")
  private Long deadLetterId;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AdminAuditLog() {}

  public AdminAuditLog(
      String operation,
      String requestPath,
      String httpMethod,
      int httpStatus,
      String outcome,
      Long repositoryId,
      Long prNumber,
      Long deadLetterId,
      String errorMessage,
      Instant createdAt) {
    this.operation = operation;
    this.requestPath = requestPath;
    this.httpMethod = httpMethod;
    this.httpStatus = httpStatus;
    this.outcome = outcome;
    this.repositoryId = repositoryId;
    this.prNumber = prNumber;
    this.deadLetterId = deadLetterId;
    this.errorMessage = errorMessage;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getOperation() {
    return operation;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getOutcome() {
    return outcome;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public Long getPrNumber() {
    return prNumber;
  }

  public Long getDeadLetterId() {
    return deadLetterId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

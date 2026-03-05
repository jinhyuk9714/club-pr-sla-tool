package com.club.sla.notify;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected DeadLetterEvent() {}

  public DeadLetterEvent(String reason, String payload, Instant createdAt) {
    this.reason = reason;
    this.payload = payload;
    this.createdAt = createdAt;
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

  public Instant getCreatedAt() {
    return createdAt;
  }
}

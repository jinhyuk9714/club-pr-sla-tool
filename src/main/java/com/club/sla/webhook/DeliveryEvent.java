package com.club.sla.webhook;

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
    name = "delivery_events",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_delivery_events_delivery_id", columnNames = "delivery_id")
    })
public class DeliveryEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "delivery_id", nullable = false, updatable = false)
  private String deliveryId;

  @Column(name = "event_type", nullable = false, updatable = false)
  private String eventType;

  @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "text")
  private String payload;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  protected DeliveryEvent() {}

  public DeliveryEvent(String deliveryId, String eventType, String payload, Instant receivedAt) {
    this.deliveryId = deliveryId;
    this.eventType = eventType;
    this.payload = payload;
    this.receivedAt = receivedAt;
  }

  public Long getId() {
    return id;
  }

  public String getDeliveryId() {
    return deliveryId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

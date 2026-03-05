package com.club.sla.webhook;

public interface WebhookIngestionPort {

  void ingest(String deliveryId, String eventType, String payload);
}

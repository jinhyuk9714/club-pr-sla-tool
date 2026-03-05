package com.club.sla.webhook;

import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryIngestionService {

  private final DeliveryEventRepository deliveryEventRepository;

  public DeliveryIngestionService(DeliveryEventRepository deliveryEventRepository) {
    this.deliveryEventRepository = deliveryEventRepository;
  }

  @Transactional
  public boolean record(String deliveryId, String eventType, String payload) {
    if (deliveryEventRepository.existsByDeliveryId(deliveryId)) {
      return false;
    }

    try {
      deliveryEventRepository.save(
          new DeliveryEvent(deliveryId, eventType, payload, Instant.now()));
      return true;
    } catch (DataIntegrityViolationException ex) {
      return false;
    }
  }
}

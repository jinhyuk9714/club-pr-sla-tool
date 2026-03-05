package com.club.sla.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {

  boolean existsByDeliveryId(String deliveryId);
}

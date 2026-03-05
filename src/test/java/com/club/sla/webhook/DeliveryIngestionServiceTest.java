package com.club.sla.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DeliveryIngestionServiceTest {

  @Mock private DeliveryEventRepository deliveryEventRepository;

  @InjectMocks private DeliveryIngestionService deliveryIngestionService;

  @Test
  void recordsDeliveryEventWhenDeliveryIdDoesNotExist() {
    when(deliveryEventRepository.existsByDeliveryId("delivery-1")).thenReturn(false);

    boolean recorded = deliveryIngestionService.record("delivery-1", "pull_request", "{}");

    assertThat(recorded).isTrue();
    verify(deliveryEventRepository).save(any(DeliveryEvent.class));
  }

  @Test
  void ignoresDeliveryEventWhenDeliveryIdAlreadyExists() {
    when(deliveryEventRepository.existsByDeliveryId("delivery-1")).thenReturn(true);

    boolean recorded = deliveryIngestionService.record("delivery-1", "pull_request", "{}");

    assertThat(recorded).isFalse();
    verify(deliveryEventRepository, never()).save(any(DeliveryEvent.class));
  }

  @Test
  void ignoresDuplicateOnUniqueConstraintRace() {
    when(deliveryEventRepository.existsByDeliveryId("delivery-1")).thenReturn(false);
    when(deliveryEventRepository.save(any(DeliveryEvent.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    boolean recorded = deliveryIngestionService.record("delivery-1", "pull_request", "{}");

    assertThat(recorded).isFalse();
  }
}

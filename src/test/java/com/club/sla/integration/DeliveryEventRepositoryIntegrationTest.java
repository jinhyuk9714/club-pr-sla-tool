package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.webhook.DeliveryEventRepository;
import com.club.sla.webhook.DeliveryIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class DeliveryEventRepositoryIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("club_sla")
          .withUsername("club_sla")
          .withPassword("club_sla");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
  }

  @Autowired private DeliveryIngestionService deliveryIngestionService;

  @Autowired private DeliveryEventRepository deliveryEventRepository;

  @Test
  void storesDeliveryOnlyOnceForDuplicateDeliveryId() {
    boolean first = deliveryIngestionService.record("delivery-1", "pull_request", "{}");
    boolean second = deliveryIngestionService.record("delivery-1", "pull_request", "{}");

    assertThat(first).isTrue();
    assertThat(second).isFalse();
    assertThat(deliveryEventRepository.count()).isEqualTo(1);
  }
}

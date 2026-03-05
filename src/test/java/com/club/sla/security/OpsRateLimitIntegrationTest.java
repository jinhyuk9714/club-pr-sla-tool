package com.club.sla.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class OpsRateLimitIntegrationTest {

  private static final String ADMIN_API_KEY = "test-admin-key";

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
    registry.add("ops.security.admin-api-key", () -> ADMIN_API_KEY);
    registry.add("ops.security.rate-limit.max-requests", () -> 2);
    registry.add("ops.security.rate-limit.window-seconds", () -> 60);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private MeterRegistry meterRegistry;

  @Test
  void returnsTooManyRequestsWhenLimitIsExceeded() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/dead-letters")
                .param("status", "PENDING")
                .param("limit", "10")
                .header("X-Admin-Api-Key", ADMIN_API_KEY))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/admin/dead-letters")
                .param("status", "PENDING")
                .param("limit", "10")
                .header("X-Admin-Api-Key", ADMIN_API_KEY))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/admin/dead-letters")
                .param("status", "PENDING")
                .param("limit", "10")
                .header("X-Admin-Api-Key", ADMIN_API_KEY))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void incrementsAuthFailureMetricWhenApiKeyIsInvalid() throws Exception {
    double before =
        meterRegistry.counter("ops_auth_failures_total", "endpoint", "admin_dead_letters").count();

    mockMvc
        .perform(
            get("/api/admin/dead-letters")
                .param("status", "PENDING")
                .param("limit", "10")
                .header("X-Admin-Api-Key", "invalid-key"))
        .andExpect(status().isUnauthorized());

    double after =
        meterRegistry.counter("ops_auth_failures_total", "endpoint", "admin_dead_letters").count();
    assertThat(after).isEqualTo(before + 1.0d);
  }
}

package com.club.sla.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class OpsApiKeyFilterIntegrationTest {

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
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsDeadLetterAdminRequestWhenHeaderIsMissing() throws Exception {
    mockMvc
        .perform(get("/api/admin/dead-letters").param("status", "PENDING").param("limit", "20"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsDeadLetterAdminRequestWhenApiKeyIsInvalid() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/dead-letters")
                .param("status", "PENDING")
                .param("limit", "20")
                .header("X-Admin-Api-Key", "wrong-key"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void allowsDeadLetterAdminRequestWhenApiKeyIsValid() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/dead-letters")
                .param("status", "PENDING")
                .param("limit", "20")
                .header("X-Admin-Api-Key", ADMIN_API_KEY))
        .andExpect(status().isOk());
  }

  @Test
  void preservesControllerStatusWhenApiKeyIsValid() throws Exception {
    mockMvc
        .perform(
            post("/api/repositories/9001/pull-requests/999/sla/re-evaluate")
                .header("X-Admin-Api-Key", ADMIN_API_KEY))
        .andExpect(status().isNotFound());
  }

  @Test
  void protectsAdminAuditLogsEndpointWithApiKey() throws Exception {
    mockMvc
        .perform(get("/api/admin/audit-logs").param("limit", "10"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            get("/api/admin/audit-logs")
                .param("limit", "10")
                .header("X-Admin-Api-Key", ADMIN_API_KEY))
        .andExpect(status().isOk());
  }

  @Test
  void keepsHealthEndpointUnprotected() throws Exception {
    mockMvc.perform(get("/api/health")).andExpect(status().isOk());
  }

  @Test
  void keepsWebhookEndpointOutsideAdminApiKeyProtection() throws Exception {
    mockMvc.perform(post("/api/webhooks/github")).andExpect(status().isBadRequest());
  }
}

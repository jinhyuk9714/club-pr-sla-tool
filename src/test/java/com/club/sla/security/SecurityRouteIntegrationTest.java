package com.club.sla.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.club.sla.github.GithubAuthenticatedUser;
import com.club.sla.onboarding.HttpSessionGithubUserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRouteIntegrationTest {

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
    registry.add("beta.access.allowed-logins", () -> "alice");
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void keepsHomePagePublic() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  void requiresAuthenticatedSessionForInstallationRoutes() throws Exception {
    mockMvc.perform(get("/app/installations/7001")).andExpect(status().isUnauthorized());
  }

  @Test
  void allowsSetupRouteToReachControllerLoginRedirect() throws Exception {
    mockMvc
        .perform(get("/app/installations/setup").param("installation_id", "7001"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/login/github?returnTo=%2Fapp%2Finstallations%2Fsetup%3Finstallation_id%3D7001"));
  }

  @Test
  void deniesPrometheusByDefaultEvenWithAuthenticatedSession() throws Exception {
    MockHttpSession mockHttpSession = new MockHttpSession();
    mockHttpSession.setAttribute(
        HttpSessionGithubUserSessionService.AUTHENTICATED_USER_SESSION_KEY,
        new GithubAuthenticatedUser(101L, "alice", "user-token"));

    mockMvc
        .perform(get("/actuator/prometheus").session(mockHttpSession))
        .andExpect(status().isUnauthorized());
  }
}

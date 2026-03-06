package com.club.sla.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class OpsUiSecurityIntegrationTest {

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
    registry.add("beta.access.allowed-logins", () -> "alice");
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void keepsOpsLoginPagePublic() throws Exception {
    mockMvc.perform(get("/ops/login")).andExpect(status().isOk());
  }

  @Test
  void redirectsAnonymousOpsRequestToLoginPage() throws Exception {
    mockMvc
        .perform(get("/ops"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops/login"));
  }

  @Test
  void redirectsGithubSessionAwayFromOpsPagesWhenOpsSessionIsMissing() throws Exception {
    MockHttpSession githubSession = new MockHttpSession();
    githubSession.setAttribute(
        HttpSessionGithubUserSessionService.AUTHENTICATED_USER_SESSION_KEY,
        new GithubAuthenticatedUser(101L, "alice", "user-token"));

    mockMvc
        .perform(get("/ops").session(githubSession))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops/login"));
  }

  @Test
  void allowsOpsSessionToAccessOpsPages() throws Exception {
    MockHttpSession opsSession = new MockHttpSession();
    opsSession.setAttribute(
        HttpSessionOpsUserSessionService.AUTHENTICATED_OPS_SESSION_KEY,
        new OpsAuthenticatedSession("ops-admin", java.time.Instant.parse("2026-03-06T00:00:00Z")));

    mockMvc.perform(get("/ops").session(opsSession)).andExpect(status().isOk());
  }

  @Test
  void doesNotAllowOpsSessionToAccessGithubInstallationRoutes() throws Exception {
    MockHttpSession opsSession = new MockHttpSession();
    opsSession.setAttribute(
        HttpSessionOpsUserSessionService.AUTHENTICATED_OPS_SESSION_KEY,
        new OpsAuthenticatedSession("ops-admin", java.time.Instant.parse("2026-03-06T00:00:00Z")));

    mockMvc
        .perform(get("/app/installations/7001").session(opsSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsOpsSessionThroughLoginForm() throws Exception {
    mockMvc
        .perform(
            post("/ops/login")
                .param("adminApiKey", ADMIN_API_KEY)
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops"));
  }
}

package com.club.sla.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.club.sla.config.RestClientConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = RestGithubAppClientPatchIntegrationTest.Config.class)
@ImportAutoConfiguration(RestClientAutoConfiguration.class)
class RestGithubAppClientPatchIntegrationTest {

  private static HttpServer httpServer;
  private static String baseUrl;
  private static final AtomicBoolean patchObserved = new AtomicBoolean(false);
  private static final AtomicReference<String> authorizationHeader = new AtomicReference<>();

  @Autowired private RestGithubAppClient restGithubAppClient;

  @Autowired private GithubAppJwtService githubAppJwtService;

  @BeforeAll
  static void setUpServer() throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress(0), 0);
    baseUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort();

    httpServer.createContext(
        "/app/installations/7001/access_tokens",
        exchange -> {
          if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
          }
          send(exchange, 200, "{\"token\":\"installation-token\"}");
        });
    httpServer.createContext(
        "/repos/club-org/club-pr-tool/commits/head-123/check-runs",
        exchange -> {
          if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
          }
          send(exchange, 200, "{\"total_count\":1,\"check_runs\":[{\"id\":2001}]}");
        });
    httpServer.createContext(
        "/repos/club-org/club-pr-tool/check-runs/2001",
        exchange -> {
          authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
          patchObserved.set("PATCH".equals(exchange.getRequestMethod()));
          send(exchange, 200, "{\"id\":2001}");
        });
    httpServer.start();
  }

  @AfterAll
  static void stopServer() {
    if (httpServer != null) {
      httpServer.stop(0);
    }
  }

  @BeforeEach
  void resetObservations() {
    patchObserved.set(false);
    authorizationHeader.set(null);
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("github.app.client-id", () -> "client-id");
    registry.add("github.app.client-secret", () -> "client-secret");
    registry.add("github.app.auth-base-url", () -> baseUrl);
    registry.add("github.app.api-base-url", () -> baseUrl);
  }

  @Test
  void updatesExistingCheckRunWithRealPatchRequest() {
    given(githubAppJwtService.createJwt()).willReturn("app-jwt");

    restGithubAppClient.upsertCheckRun(
        7001L,
        "club-org/club-pr-tool",
        new GithubCheckRunUpsertCommand(
            "Club PR SLA",
            "head-123",
            "pr-77",
            "completed",
            "success",
            "Reviewed",
            "A first review has been recorded.",
            "SLA tracking is complete for this revision.",
            Instant.parse("2026-03-06T00:00:00Z"),
            Instant.parse("2026-03-06T00:00:00Z")));

    assertThat(patchObserved).isTrue();
    assertThat(authorizationHeader.get()).isEqualTo("Bearer installation-token");
  }

  @TestConfiguration
  @Import({RestGithubAppClient.class, RestClientConfiguration.class})
  static class Config {

    @Bean
    GithubAppJwtService githubAppJwtService() {
      return Mockito.mock(GithubAppJwtService.class);
    }
  }

  private static void send(HttpExchange exchange, int status, String body) throws IOException {
    byte[] payload = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, payload.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(payload);
    }
  }
}

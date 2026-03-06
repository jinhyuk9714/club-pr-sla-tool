package com.club.sla.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.metrics.OpsSecurityMetrics;
import com.club.sla.security.OpsRateLimitDecision;
import com.club.sla.security.OpsRateLimiter;
import com.club.sla.security.OpsSecurityProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpsUiAuthenticationServiceTest {

  @Mock private OpsRateLimiter opsRateLimiter;
  @Mock private OpsSecurityMetrics opsSecurityMetrics;
  @Mock private OpsUserSessionService opsUserSessionService;

  private OpsSecurityProperties opsSecurityProperties;
  private OpsUiAuthenticationService opsUiAuthenticationService;

  @BeforeEach
  void setUp() {
    opsSecurityProperties = new OpsSecurityProperties();
    opsSecurityProperties.setAdminApiKey("test-admin-key");
    opsUiAuthenticationService =
        new OpsUiAuthenticationService(
            opsSecurityProperties,
            opsRateLimiter,
            opsSecurityMetrics,
            opsUserSessionService,
            Clock.fixed(Instant.parse("2026-03-06T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void storesOpsSessionWhenApiKeyMatches() {
    when(opsRateLimiter.check("127.0.0.1", "ops_ui_login"))
        .thenReturn(OpsRateLimitDecision.allow());

    OpsUiLoginResult result =
        opsUiAuthenticationService.authenticate("test-admin-key", "127.0.0.1");

    assertThat(result.authenticated()).isTrue();
    assertThat(result.rateLimited()).isFalse();
    verify(opsUserSessionService).storeAuthenticatedSession(any(OpsAuthenticatedSession.class));
    verify(opsSecurityMetrics, never()).incrementAuthFailure("ops_ui_login");
    verify(opsSecurityMetrics, never()).incrementRateLimited("ops_ui_login");
  }

  @Test
  void rejectsLoginWhenApiKeyDoesNotMatch() {
    when(opsRateLimiter.check("127.0.0.1", "ops_ui_login"))
        .thenReturn(OpsRateLimitDecision.allow());

    OpsUiLoginResult result = opsUiAuthenticationService.authenticate("wrong-key", "127.0.0.1");

    assertThat(result.authenticated()).isFalse();
    assertThat(result.rateLimited()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("Invalid admin API key.");
    verify(opsUserSessionService, never()).storeAuthenticatedSession(any());
    verify(opsSecurityMetrics).incrementAuthFailure("ops_ui_login");
  }

  @Test
  void rejectsLoginWhenRateLimitIsExceeded() {
    when(opsRateLimiter.check("127.0.0.1", "ops_ui_login"))
        .thenReturn(OpsRateLimitDecision.deny(42));

    OpsUiLoginResult result =
        opsUiAuthenticationService.authenticate("test-admin-key", "127.0.0.1");

    assertThat(result.authenticated()).isFalse();
    assertThat(result.rateLimited()).isTrue();
    assertThat(result.retryAfterSeconds()).isEqualTo(42);
    verify(opsUserSessionService, never()).storeAuthenticatedSession(any());
    verify(opsSecurityMetrics).incrementRateLimited("ops_ui_login");
    verify(opsSecurityMetrics, never()).incrementAuthFailure("ops_ui_login");
  }
}

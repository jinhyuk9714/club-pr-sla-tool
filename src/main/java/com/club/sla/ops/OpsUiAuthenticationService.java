package com.club.sla.ops;

import com.club.sla.metrics.OpsSecurityMetrics;
import com.club.sla.security.OpsRateLimitDecision;
import com.club.sla.security.OpsRateLimiter;
import com.club.sla.security.OpsSecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpsUiAuthenticationService {

  private static final String ENDPOINT_GROUP = "ops_ui_login";

  private final OpsSecurityProperties opsSecurityProperties;
  private final OpsRateLimiter opsRateLimiter;
  private final OpsSecurityMetrics opsSecurityMetrics;
  private final OpsUserSessionService opsUserSessionService;
  private final Clock clock;

  @Autowired
  public OpsUiAuthenticationService(
      OpsSecurityProperties opsSecurityProperties,
      OpsRateLimiter opsRateLimiter,
      OpsSecurityMetrics opsSecurityMetrics,
      OpsUserSessionService opsUserSessionService) {
    this(
        opsSecurityProperties,
        opsRateLimiter,
        opsSecurityMetrics,
        opsUserSessionService,
        Clock.systemUTC());
  }

  OpsUiAuthenticationService(
      OpsSecurityProperties opsSecurityProperties,
      OpsRateLimiter opsRateLimiter,
      OpsSecurityMetrics opsSecurityMetrics,
      OpsUserSessionService opsUserSessionService,
      Clock clock) {
    this.opsSecurityProperties =
        Objects.requireNonNull(opsSecurityProperties, "opsSecurityProperties");
    this.opsRateLimiter = Objects.requireNonNull(opsRateLimiter, "opsRateLimiter");
    this.opsSecurityMetrics = Objects.requireNonNull(opsSecurityMetrics, "opsSecurityMetrics");
    this.opsUserSessionService =
        Objects.requireNonNull(opsUserSessionService, "opsUserSessionService");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public OpsUiLoginResult authenticate(String providedApiKey, String remoteAddress) {
    OpsRateLimitDecision rateLimitDecision =
        opsRateLimiter.check(normalizeRemoteAddress(remoteAddress), ENDPOINT_GROUP);
    if (!rateLimitDecision.allowed()) {
      opsSecurityMetrics.incrementRateLimited(ENDPOINT_GROUP);
      return OpsUiLoginResult.rateLimited(rateLimitDecision.retryAfterSeconds());
    }

    if (!isValidApiKey(opsSecurityProperties.getAdminApiKey(), providedApiKey)) {
      opsSecurityMetrics.incrementAuthFailure(ENDPOINT_GROUP);
      return OpsUiLoginResult.invalidCredentials();
    }

    opsUserSessionService.storeAuthenticatedSession(
        new OpsAuthenticatedSession("ops-admin", Instant.now(clock)));
    return OpsUiLoginResult.success();
  }

  public void logout() {
    opsUserSessionService.clear();
  }

  private String normalizeRemoteAddress(String remoteAddress) {
    return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
  }

  private boolean isValidApiKey(String configuredApiKey, String requestApiKey) {
    if (configuredApiKey == null || configuredApiKey.isBlank()) {
      return false;
    }
    if (requestApiKey == null || requestApiKey.isBlank()) {
      return false;
    }
    byte[] expected = configuredApiKey.getBytes(StandardCharsets.UTF_8);
    byte[] actual = requestApiKey.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, actual);
  }
}

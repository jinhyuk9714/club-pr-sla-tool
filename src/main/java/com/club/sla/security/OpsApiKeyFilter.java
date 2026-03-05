package com.club.sla.security;

import com.club.sla.metrics.OpsSecurityMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OpsApiKeyFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "X-Admin-Api-Key";
  private static final Pattern MANUAL_REEVALUATE_PATH =
      Pattern.compile("^/api/repositories/[^/]+/pull-requests/[^/]+/sla/re-evaluate$");
  private static final Pattern DEAD_LETTER_REPLAY_PATH =
      Pattern.compile("^/api/admin/dead-letters/[^/]+/replay$");

  private final OpsSecurityProperties opsSecurityProperties;
  private final OpsRateLimiter opsRateLimiter;
  private final OpsSecurityMetrics opsSecurityMetrics;

  public OpsApiKeyFilter(
      ObjectProvider<OpsSecurityProperties> opsSecurityPropertiesProvider,
      ObjectProvider<OpsRateLimiter> opsRateLimiterProvider,
      ObjectProvider<OpsSecurityMetrics> opsSecurityMetricsProvider) {
    this.opsSecurityProperties =
        opsSecurityPropertiesProvider.getIfAvailable(OpsSecurityProperties::new);
    this.opsRateLimiter =
        opsRateLimiterProvider.getIfAvailable(() -> new OpsRateLimiter(this.opsSecurityProperties));
    this.opsSecurityMetrics = opsSecurityMetricsProvider.getIfAvailable();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isProtectedEndpoint(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String configuredApiKey = opsSecurityProperties.getAdminApiKey();
    String requestApiKey = request.getHeader(HEADER_NAME);
    String endpointGroup = endpointGroup(request);
    if (!isValidApiKey(configuredApiKey, requestApiKey)) {
      incrementAuthFailure(endpointGroup);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    OpsRateLimitDecision rateLimitDecision = opsRateLimiter.check(requestApiKey, endpointGroup);
    if (!rateLimitDecision.allowed()) {
      incrementRateLimited(endpointGroup);
      response.setHeader("Retry-After", Long.toString(rateLimitDecision.retryAfterSeconds()));
      response.setStatus(429);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isProtectedEndpoint(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String method = request.getMethod();

    if (requestUri.startsWith("/api/admin/")) {
      return "GET".equals(method) || "POST".equals(method);
    }
    return "POST".equals(method) && MANUAL_REEVALUATE_PATH.matcher(requestUri).matches();
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

  private String endpointGroup(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    if ("/api/admin/dead-letters".equals(requestUri)) {
      return "admin_dead_letters";
    }
    if ("/api/admin/audit-logs".equals(requestUri)) {
      return "admin_audit_logs";
    }
    if (DEAD_LETTER_REPLAY_PATH.matcher(requestUri).matches()) {
      return "admin_dead_letter_replay";
    }
    if (MANUAL_REEVALUATE_PATH.matcher(requestUri).matches()) {
      return "manual_reevaluate";
    }
    return "ops_unknown";
  }

  private void incrementAuthFailure(String endpointGroup) {
    if (opsSecurityMetrics != null) {
      opsSecurityMetrics.incrementAuthFailure(endpointGroup);
    }
  }

  private void incrementRateLimited(String endpointGroup) {
    if (opsSecurityMetrics != null) {
      opsSecurityMetrics.incrementRateLimited(endpointGroup);
    }
  }
}

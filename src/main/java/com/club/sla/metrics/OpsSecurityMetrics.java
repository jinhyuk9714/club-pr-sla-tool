package com.club.sla.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OpsSecurityMetrics {

  private final MeterRegistry meterRegistry;
  private final Map<String, Counter> authFailureCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> rateLimitedCounters = new ConcurrentHashMap<>();

  public OpsSecurityMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void incrementAuthFailure(String endpointGroup) {
    authFailureCounters.computeIfAbsent(endpointGroup, this::newAuthFailureCounter).increment();
  }

  public void incrementRateLimited(String endpointGroup) {
    rateLimitedCounters.computeIfAbsent(endpointGroup, this::newRateLimitedCounter).increment();
  }

  private Counter newAuthFailureCounter(String endpointGroup) {
    return Counter.builder("ops_auth_failures_total")
        .tag("endpoint", endpointGroup)
        .register(meterRegistry);
  }

  private Counter newRateLimitedCounter(String endpointGroup) {
    return Counter.builder("ops_rate_limited_total")
        .tag("endpoint", endpointGroup)
        .register(meterRegistry);
  }
}

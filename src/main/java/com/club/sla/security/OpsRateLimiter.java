package com.club.sla.security;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpsRateLimiter {

  private final int maxRequests;
  private final int windowSeconds;
  private final Clock clock;
  private final Map<String, WindowCounter> counters = new HashMap<>();

  @Autowired
  public OpsRateLimiter(OpsSecurityProperties opsSecurityProperties) {
    this(
        opsSecurityProperties.getRateLimit().getMaxRequests(),
        opsSecurityProperties.getRateLimit().getWindowSeconds(),
        Clock.systemUTC());
  }

  OpsRateLimiter(int maxRequests, int windowSeconds, Clock clock) {
    this.maxRequests = maxRequests > 0 ? maxRequests : 60;
    this.windowSeconds = windowSeconds > 0 ? windowSeconds : 60;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public synchronized OpsRateLimitDecision check(String apiKey, String endpointGroup) {
    long nowEpochSeconds = clock.instant().getEpochSecond();
    long windowStartEpochSeconds = (nowEpochSeconds / windowSeconds) * windowSeconds;
    String bucketKey = endpointGroup + "|" + apiKey;

    WindowCounter counter = counters.get(bucketKey);
    if (counter == null || counter.windowStartEpochSeconds != windowStartEpochSeconds) {
      counters.put(bucketKey, new WindowCounter(windowStartEpochSeconds, 1));
      return OpsRateLimitDecision.allow();
    }

    if (counter.requestCount < maxRequests) {
      counters.put(bucketKey, new WindowCounter(windowStartEpochSeconds, counter.requestCount + 1));
      return OpsRateLimitDecision.allow();
    }

    long retryAfterSeconds = (windowStartEpochSeconds + windowSeconds) - nowEpochSeconds;
    if (retryAfterSeconds <= 0) {
      retryAfterSeconds = 1;
    }
    return OpsRateLimitDecision.deny(retryAfterSeconds);
  }

  private static final class WindowCounter {
    private final long windowStartEpochSeconds;
    private final int requestCount;

    private WindowCounter(long windowStartEpochSeconds, int requestCount) {
      this.windowStartEpochSeconds = windowStartEpochSeconds;
      this.requestCount = requestCount;
    }
  }
}

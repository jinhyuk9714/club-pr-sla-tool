package com.club.sla.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OpsRateLimiterTest {

  @Test
  void blocksWhenRequestsExceedWindowLimit() {
    MutableClock clock = new MutableClock(Instant.parse("2026-03-06T00:00:00Z"));
    OpsRateLimiter limiter = new OpsRateLimiter(2, 60, clock);

    OpsRateLimitDecision first = limiter.check("key-1", "admin_dead_letters");
    OpsRateLimitDecision second = limiter.check("key-1", "admin_dead_letters");
    OpsRateLimitDecision third = limiter.check("key-1", "admin_dead_letters");

    assertThat(first.allowed()).isTrue();
    assertThat(second.allowed()).isTrue();
    assertThat(third.allowed()).isFalse();
    assertThat(third.retryAfterSeconds()).isEqualTo(60);
  }

  @Test
  void allowsAgainWhenWindowExpires() {
    MutableClock clock = new MutableClock(Instant.parse("2026-03-06T00:00:00Z"));
    OpsRateLimiter limiter = new OpsRateLimiter(1, 60, clock);

    assertThat(limiter.check("key-2", "manual_reevaluate").allowed()).isTrue();
    assertThat(limiter.check("key-2", "manual_reevaluate").allowed()).isFalse();

    clock.set(Instant.parse("2026-03-06T00:01:01Z"));

    OpsRateLimitDecision afterReset = limiter.check("key-2", "manual_reevaluate");
    assertThat(afterReset.allowed()).isTrue();
    assertThat(afterReset.retryAfterSeconds()).isEqualTo(0);
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    private void set(Instant instant) {
      this.instant = instant;
    }
  }
}

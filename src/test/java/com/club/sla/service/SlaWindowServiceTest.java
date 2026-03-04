package com.club.sla.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SlaWindowServiceTest {

  @Test
  void calculatesElapsedHoursFromReadyTimestamp() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = Instant.parse("2026-03-05T15:30:00Z");

    Duration elapsed = SlaWindowService.elapsed(readyAt, now);

    assertThat(elapsed).isEqualTo(Duration.ofHours(15).plusMinutes(30));
  }
}

package com.club.sla.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OutboundDeliveryMetricsIntegrationTest {

  @Test
  void incrementsAllOutboundDeliveryMetricCounters() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    OutboundDeliveryMetrics outboundDeliveryMetrics = new OutboundDeliveryMetrics(meterRegistry);

    outboundDeliveryMetrics.incrementCreated(OutboundDeliveryJobType.DISCORD_NOTIFICATION);
    outboundDeliveryMetrics.incrementSucceeded(OutboundDeliveryJobType.DISCORD_NOTIFICATION);
    outboundDeliveryMetrics.incrementAttemptFailure(OutboundDeliveryJobType.GITHUB_CHECK_SYNC);
    outboundDeliveryMetrics.incrementDead(OutboundDeliveryJobType.GITHUB_CHECK_SYNC);

    assertThat(
            counterValue(
                meterRegistry, "outbound_jobs_created_total", "type", "DISCORD_NOTIFICATION"))
        .isEqualTo(1.0);
    assertThat(
            counterValue(
                meterRegistry, "outbound_jobs_succeeded_total", "type", "DISCORD_NOTIFICATION"))
        .isEqualTo(1.0);
    assertThat(
            counterValue(
                meterRegistry, "outbound_job_attempt_failures_total", "type", "GITHUB_CHECK_SYNC"))
        .isEqualTo(1.0);
    assertThat(counterValue(meterRegistry, "outbound_jobs_dead_total", "type", "GITHUB_CHECK_SYNC"))
        .isEqualTo(1.0);
  }

  private double counterValue(MeterRegistry meterRegistry, String name, String... tags) {
    return meterRegistry.get(name).tags(tags).counter().count();
  }
}

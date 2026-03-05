package com.club.sla.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.club.sla.sla.SlaAction;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class SlaMetricsIntegrationTest {

  @Test
  void incrementsAllSlaMetricCounters() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    SlaMetrics slaMetrics = new SlaMetrics(meterRegistry);

    slaMetrics.incrementNotification(SlaAction.REMIND_12H);
    slaMetrics.incrementScanRun();
    slaMetrics.incrementScanFailure();

    assertThat(counterValue(meterRegistry, "sla_notifications_total", "stage", "REMIND_12H"))
        .isEqualTo(1.0);
    assertThat(counterValue(meterRegistry, "sla_scan_runs_total")).isEqualTo(1.0);
    assertThat(counterValue(meterRegistry, "sla_scan_failures_total")).isEqualTo(1.0);
  }

  private double counterValue(MeterRegistry meterRegistry, String name, String... tags) {
    return meterRegistry.get(name).tags(tags).counter().count();
  }
}

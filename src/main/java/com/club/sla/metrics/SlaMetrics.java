package com.club.sla.metrics;

import com.club.sla.sla.SlaAction;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SlaMetrics {

  private final Counter scanRunsCounter;
  private final Counter scanFailuresCounter;
  private final Map<SlaAction, Counter> notificationCounters;

  public SlaMetrics(MeterRegistry meterRegistry) {
    this.scanRunsCounter = Counter.builder("sla_scan_runs_total").register(meterRegistry);
    this.scanFailuresCounter = Counter.builder("sla_scan_failures_total").register(meterRegistry);
    this.notificationCounters = new EnumMap<>(SlaAction.class);
    for (SlaAction action : SlaAction.values()) {
      Counter counter =
          Counter.builder("sla_notifications_total")
              .tag("stage", action.name())
              .register(meterRegistry);
      this.notificationCounters.put(action, counter);
    }
  }

  public void incrementNotification(SlaAction action) {
    Counter counter = notificationCounters.get(action);
    if (counter == null) {
      return;
    }
    counter.increment();
  }

  public void incrementScanRun() {
    scanRunsCounter.increment();
  }

  public void incrementScanFailure() {
    scanFailuresCounter.increment();
  }
}

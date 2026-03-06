package com.club.sla.delivery;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OutboundDeliveryMetrics {

  private final Map<OutboundDeliveryJobType, Counter> createdCounters =
      new EnumMap<>(OutboundDeliveryJobType.class);
  private final Map<OutboundDeliveryJobType, Counter> succeededCounters =
      new EnumMap<>(OutboundDeliveryJobType.class);
  private final Map<OutboundDeliveryJobType, Counter> deadCounters =
      new EnumMap<>(OutboundDeliveryJobType.class);
  private final Map<OutboundDeliveryJobType, Counter> attemptFailureCounters =
      new EnumMap<>(OutboundDeliveryJobType.class);

  public OutboundDeliveryMetrics(MeterRegistry meterRegistry) {
    for (OutboundDeliveryJobType jobType : OutboundDeliveryJobType.values()) {
      createdCounters.put(
          jobType,
          Counter.builder("outbound_jobs_created_total")
              .tag("type", jobType.name())
              .register(meterRegistry));
      succeededCounters.put(
          jobType,
          Counter.builder("outbound_jobs_succeeded_total")
              .tag("type", jobType.name())
              .register(meterRegistry));
      deadCounters.put(
          jobType,
          Counter.builder("outbound_jobs_dead_total")
              .tag("type", jobType.name())
              .register(meterRegistry));
      attemptFailureCounters.put(
          jobType,
          Counter.builder("outbound_job_attempt_failures_total")
              .tag("type", jobType.name())
              .register(meterRegistry));
    }
  }

  public void incrementCreated(OutboundDeliveryJobType jobType) {
    createdCounters.get(jobType).increment();
  }

  public void incrementSucceeded(OutboundDeliveryJobType jobType) {
    succeededCounters.get(jobType).increment();
  }

  public void incrementDead(OutboundDeliveryJobType jobType) {
    deadCounters.get(jobType).increment();
  }

  public void incrementAttemptFailure(OutboundDeliveryJobType jobType) {
    attemptFailureCounters.get(jobType).increment();
  }
}

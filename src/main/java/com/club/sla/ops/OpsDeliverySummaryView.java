package com.club.sla.ops;

import com.club.sla.delivery.OutboundDeliveryJobStatus;
import com.club.sla.delivery.OutboundDeliveryJobType;
import java.time.Instant;

public record OpsDeliverySummaryView(
    Long id,
    OutboundDeliveryJobType jobType,
    OutboundDeliveryJobStatus status,
    Long installationId,
    Long repositoryId,
    Long prNumber,
    int attemptCount,
    Instant nextAttemptAt,
    String lastError,
    Instant createdAt) {}

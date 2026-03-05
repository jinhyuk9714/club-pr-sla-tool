package com.club.sla.notify;

import com.club.sla.sla.SlaAction;
import java.time.Instant;

public record DeadLetterSummaryDto(
    Long id,
    String reason,
    String payload,
    Long repoId,
    Long prNumber,
    SlaAction stage,
    DeadLetterReplayStatus replayStatus,
    int replayAttempts,
    String lastError,
    Instant createdAt,
    Instant replayedAt,
    Instant updatedAt) {}

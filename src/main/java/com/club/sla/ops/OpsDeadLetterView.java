package com.club.sla.ops;

import com.club.sla.notify.DeadLetterReplayStatus;
import java.time.Instant;

public record OpsDeadLetterView(
    Long id,
    String reason,
    Long repoId,
    Long prNumber,
    String stage,
    DeadLetterReplayStatus replayStatus,
    int replayAttempts,
    String lastError,
    Instant createdAt,
    Instant replayedAt) {}

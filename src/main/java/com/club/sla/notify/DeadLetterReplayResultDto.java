package com.club.sla.notify;

import java.time.Instant;

public record DeadLetterReplayResultDto(
    Long id,
    DeadLetterReplayStatus replayStatus,
    int replayAttempts,
    String lastError,
    Instant replayedAt,
    Instant updatedAt) {}

package com.club.sla.sla;

import java.time.Instant;

public record SlaReevaluationResultDto(
    boolean actionDispatched, SlaAction action, String reason, Instant evaluatedAt) {}

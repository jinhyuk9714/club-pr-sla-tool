package com.club.sla.github;

import java.time.Instant;

public record GithubCheckRunUpsertCommand(
    String checkName,
    String headSha,
    String externalId,
    String status,
    String conclusion,
    String title,
    String summary,
    String text,
    Instant startedAt,
    Instant completedAt) {}

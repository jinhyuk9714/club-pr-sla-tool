package com.club.sla.audit;

import java.time.Instant;

public record AdminAuditLogDto(
    Long id,
    String operation,
    String requestPath,
    String httpMethod,
    int httpStatus,
    String outcome,
    Long repositoryId,
    Long prNumber,
    Long deadLetterId,
    String errorMessage,
    Instant createdAt) {}

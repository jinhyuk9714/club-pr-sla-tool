package com.club.sla.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditLogService {

  private final AdminAuditLogRepository adminAuditLogRepository;
  private final Clock clock;

  @Autowired
  public AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository) {
    this(adminAuditLogRepository, Clock.systemUTC());
  }

  AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository, Clock clock) {
    this.adminAuditLogRepository = adminAuditLogRepository;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Transactional
  public void record(
      String operation,
      String requestPath,
      String httpMethod,
      int httpStatus,
      String outcome,
      Long repositoryId,
      Long prNumber,
      Long deadLetterId,
      String errorMessage) {
    adminAuditLogRepository.save(
        new AdminAuditLog(
            operation,
            requestPath,
            httpMethod,
            httpStatus,
            outcome,
            repositoryId,
            prNumber,
            deadLetterId,
            errorMessage,
            Instant.now(clock)));
  }

  @Transactional(readOnly = true)
  public List<AdminAuditLogDto> list(String operation, int limit) {
    int safeLimit = limit > 0 ? limit : 50;
    List<AdminAuditLog> rows;
    if (operation == null || operation.isBlank()) {
      rows = adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
    } else {
      rows =
          adminAuditLogRepository.findByOperationOrderByCreatedAtDesc(
              operation, PageRequest.of(0, safeLimit));
    }
    return rows.stream().map(this::toDto).toList();
  }

  private AdminAuditLogDto toDto(AdminAuditLog row) {
    return new AdminAuditLogDto(
        row.getId(),
        row.getOperation(),
        row.getRequestPath(),
        row.getHttpMethod(),
        row.getHttpStatus(),
        row.getOutcome(),
        row.getRepositoryId(),
        row.getPrNumber(),
        row.getDeadLetterId(),
        row.getErrorMessage(),
        row.getCreatedAt());
  }
}

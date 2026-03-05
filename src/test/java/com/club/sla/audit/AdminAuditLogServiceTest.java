package com.club.sla.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

  @Mock private AdminAuditLogRepository adminAuditLogRepository;

  private AdminAuditLogService adminAuditLogService;

  @BeforeEach
  void setUp() {
    adminAuditLogService =
        new AdminAuditLogService(
            adminAuditLogRepository,
            Clock.fixed(Instant.parse("2026-03-06T02:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void recordsAuditLogWithAllFields() {
    adminAuditLogService.record(
        "DEAD_LETTER_REPLAY",
        "/api/admin/dead-letters/10/replay",
        "POST",
        502,
        "UPSTREAM_FAILED",
        11L,
        22L,
        10L,
        "send failed");

    ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
    verify(adminAuditLogRepository).save(captor.capture());

    AdminAuditLog saved = captor.getValue();
    assertThat(saved.getOperation()).isEqualTo("DEAD_LETTER_REPLAY");
    assertThat(saved.getHttpStatus()).isEqualTo(502);
    assertThat(saved.getOutcome()).isEqualTo("UPSTREAM_FAILED");
    assertThat(saved.getRepositoryId()).isEqualTo(11L);
    assertThat(saved.getPrNumber()).isEqualTo(22L);
    assertThat(saved.getDeadLetterId()).isEqualTo(10L);
    assertThat(saved.getErrorMessage()).contains("send failed");
    assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-03-06T02:00:00Z"));
  }

  @Test
  void listsByOperationInDescendingCreatedOrder() {
    when(adminAuditLogRepository.findByOperationOrderByCreatedAtDesc(
            "SLA_REEVALUATE", PageRequest.of(0, 10)))
        .thenReturn(
            List.of(
                new AdminAuditLog(
                    "SLA_REEVALUATE",
                    "/api/repositories/1/pull-requests/2/sla/re-evaluate",
                    "POST",
                    200,
                    "SUCCESS",
                    1L,
                    2L,
                    null,
                    null,
                    Instant.parse("2026-03-06T03:00:00Z"))));

    List<AdminAuditLogDto> rows = adminAuditLogService.list("SLA_REEVALUATE", 10);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).operation()).isEqualTo("SLA_REEVALUATE");
  }

  @Test
  void listsAllWhenOperationIsNull() {
    when(adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)))
        .thenReturn(
            List.of(
                new AdminAuditLog(
                    "DEAD_LETTER_LIST",
                    "/api/admin/dead-letters",
                    "GET",
                    200,
                    "SUCCESS",
                    null,
                    null,
                    null,
                    null,
                    Instant.parse("2026-03-06T04:00:00Z"))));

    List<AdminAuditLogDto> rows = adminAuditLogService.list(null, 5);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).operation()).isEqualTo("DEAD_LETTER_LIST");
    verify(adminAuditLogRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5));
  }
}

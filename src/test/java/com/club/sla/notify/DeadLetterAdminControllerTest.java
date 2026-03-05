package com.club.sla.notify;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.club.sla.audit.AdminAuditLogService;
import com.club.sla.sla.SlaAction;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeadLetterAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeadLetterAdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DeadLetterReplayService deadLetterReplayService;
  @MockBean private AdminAuditLogService adminAuditLogService;

  @Test
  void returnsDeadLettersWithStatusFilterAndLimit() throws Exception {
    DeadLetterSummaryDto summary =
        new DeadLetterSummaryDto(
            10L,
            "DISCORD_SEND_FAILED",
            "[SLA] repo=1 pr=2 stage=REMIND_12H",
            1L,
            2L,
            SlaAction.REMIND_12H,
            DeadLetterReplayStatus.PENDING,
            0,
            null,
            Instant.parse("2026-03-06T00:00:00Z"),
            null,
            Instant.parse("2026-03-06T00:00:00Z"));
    given(deadLetterReplayService.list(DeadLetterReplayStatus.PENDING, 20))
        .willReturn(List.of(summary));

    mockMvc
        .perform(get("/api/admin/dead-letters").param("status", "PENDING").param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].replayStatus").value("PENDING"));
    verify(adminAuditLogService)
        .record(
            "DEAD_LETTER_LIST",
            "/api/admin/dead-letters",
            "GET",
            200,
            "SUCCESS",
            null,
            null,
            null,
            null);
  }

  @Test
  void returnsOkWhenReplaySucceeds() throws Exception {
    given(deadLetterReplayService.replay(10L))
        .willReturn(
            new DeadLetterReplayResultDto(
                10L,
                DeadLetterReplayStatus.REPLAYED,
                1,
                null,
                Instant.parse("2026-03-06T00:10:00Z"),
                Instant.parse("2026-03-06T00:10:00Z")));

    mockMvc
        .perform(post("/api/admin/dead-letters/10/replay"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.replayStatus").value("REPLAYED"));
    verify(adminAuditLogService)
        .record(
            "DEAD_LETTER_REPLAY",
            "/api/admin/dead-letters/10/replay",
            "POST",
            200,
            "SUCCESS",
            null,
            null,
            10L,
            null);
  }

  @Test
  void returnsNotFoundWhenDeadLetterDoesNotExist() throws Exception {
    given(deadLetterReplayService.replay(404L))
        .willThrow(new DeadLetterReplayService.DeadLetterNotFoundException(404L));

    mockMvc.perform(post("/api/admin/dead-letters/404/replay")).andExpect(status().isNotFound());
    verify(adminAuditLogService)
        .record(
            "DEAD_LETTER_REPLAY",
            "/api/admin/dead-letters/404/replay",
            "POST",
            404,
            "NOT_FOUND",
            null,
            null,
            404L,
            "dead letter not found: 404");
  }

  @Test
  void returnsConflictWhenDeadLetterIsAlreadyReplayed() throws Exception {
    given(deadLetterReplayService.replay(11L))
        .willThrow(new DeadLetterReplayService.DeadLetterAlreadyReplayedException(11L));

    mockMvc.perform(post("/api/admin/dead-letters/11/replay")).andExpect(status().isConflict());
  }

  @Test
  void returnsUnprocessableEntityWhenLegacyDeadLetterCannotBeReplayed() throws Exception {
    given(deadLetterReplayService.replay(12L))
        .willThrow(new DeadLetterReplayService.DeadLetterLegacyMetadataMissingException(12L));

    mockMvc
        .perform(post("/api/admin/dead-letters/12/replay"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void returnsBadGatewayWhenReplayDeliveryFails() throws Exception {
    given(deadLetterReplayService.replay(13L))
        .willThrow(
            new DeadLetterReplayService.DeadLetterReplayFailedException(
                new DeadLetterReplayResultDto(
                    13L,
                    DeadLetterReplayStatus.FAILED,
                    1,
                    "send failed",
                    null,
                    Instant.parse("2026-03-06T00:11:00Z"))));

    mockMvc.perform(post("/api/admin/dead-letters/13/replay")).andExpect(status().isBadGateway());
  }
}

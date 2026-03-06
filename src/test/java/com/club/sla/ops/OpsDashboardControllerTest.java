package com.club.sla.ops;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.club.sla.audit.AdminAuditLogService;
import com.club.sla.delivery.OutboundDeliveryJobStatus;
import com.club.sla.delivery.OutboundDeliveryJobType;
import com.club.sla.installation.GithubInstallationStatus;
import com.club.sla.notify.DeadLetterReplayResultDto;
import com.club.sla.notify.DeadLetterReplayService;
import com.club.sla.notify.DeadLetterReplayStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpsDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpsDashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OpsOverviewService opsOverviewService;
  @MockBean private OpsInstallationSummaryService opsInstallationSummaryService;
  @MockBean private OpsDeliveryQueryService opsDeliveryQueryService;
  @MockBean private DeadLetterReplayService deadLetterReplayService;
  @MockBean private AdminAuditLogService adminAuditLogService;

  @Test
  void rendersOpsOverviewPage() throws Exception {
    OpsOverviewView overview = new OpsOverviewView(2L, 1L, 1L, 3L, 1L, 2L);
    given(opsOverviewService.loadOverview()).willReturn(overview);

    mockMvc
        .perform(get("/ops"))
        .andExpect(status().isOk())
        .andExpect(view().name("ops-overview"))
        .andExpect(model().attribute("overview", overview));
  }

  @Test
  void rendersInstallationSummaries() throws Exception {
    OpsInstallationSummaryView installation =
        new OpsInstallationSummaryView(
            7001L,
            "club-org",
            GithubInstallationStatus.ACTIVE,
            true,
            2L,
            Instant.parse("2026-03-06T00:00:00Z"));
    given(opsInstallationSummaryService.listInstallations()).willReturn(List.of(installation));

    mockMvc
        .perform(get("/ops/installations"))
        .andExpect(status().isOk())
        .andExpect(view().name("ops-installations"))
        .andExpect(model().attribute("installations", List.of(installation)));
  }

  @Test
  void rendersFilteredDeliveries() throws Exception {
    OpsDeliverySummaryView delivery =
        new OpsDeliverySummaryView(
            11L,
            OutboundDeliveryJobType.DISCORD_NOTIFICATION,
            OutboundDeliveryJobStatus.PENDING,
            7001L,
            9001L,
            17L,
            1,
            Instant.parse("2026-03-06T00:05:00Z"),
            "send failed",
            Instant.parse("2026-03-06T00:00:00Z"));
    given(
            opsDeliveryQueryService.listRecentDeliveries(
                OutboundDeliveryJobStatus.PENDING,
                OutboundDeliveryJobType.DISCORD_NOTIFICATION,
                7001L))
        .willReturn(List.of(delivery));

    mockMvc
        .perform(
            get("/ops/deliveries")
                .param("status", "PENDING")
                .param("jobType", "DISCORD_NOTIFICATION")
                .param("installationId", "7001"))
        .andExpect(status().isOk())
        .andExpect(view().name("ops-deliveries"))
        .andExpect(model().attribute("deliveries", List.of(delivery)))
        .andExpect(model().attribute("selectedStatus", OutboundDeliveryJobStatus.PENDING))
        .andExpect(
            model().attribute("selectedJobType", OutboundDeliveryJobType.DISCORD_NOTIFICATION))
        .andExpect(model().attribute("selectedInstallationId", 7001L));
  }

  @Test
  void rendersDeadLettersWithSelectedStatus() throws Exception {
    OpsDeadLetterView deadLetter =
        new OpsDeadLetterView(
            10L,
            "DISCORD_NOTIFICATION_DEAD",
            9001L,
            17L,
            "REMIND_12H",
            DeadLetterReplayStatus.PENDING,
            0,
            "send failed",
            Instant.parse("2026-03-06T00:00:00Z"),
            null);
    given(deadLetterReplayService.list(DeadLetterReplayStatus.PENDING, 100))
        .willReturn(
            List.of(
                new com.club.sla.notify.DeadLetterSummaryDto(
                    10L,
                    "DISCORD_NOTIFICATION_DEAD",
                    "{}",
                    9001L,
                    17L,
                    com.club.sla.sla.SlaAction.REMIND_12H,
                    DeadLetterReplayStatus.PENDING,
                    0,
                    "send failed",
                    Instant.parse("2026-03-06T00:00:00Z"),
                    null,
                    Instant.parse("2026-03-06T00:00:00Z"))));

    mockMvc
        .perform(get("/ops/dead-letters").param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(view().name("ops-dead-letters"))
        .andExpect(model().attribute("deadLetters", List.of(deadLetter)))
        .andExpect(model().attribute("selectedStatus", DeadLetterReplayStatus.PENDING));
  }

  @Test
  void redirectsWithSuccessFlashWhenReplaySucceeds() throws Exception {
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
        .perform(post("/ops/dead-letters/10/replay").param("status", "PENDING"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops/dead-letters?status=PENDING"))
        .andExpect(flash().attribute("successMessage", "Dead letter replay enqueued."));
  }

  @Test
  void redirectsWithErrorFlashWhenReplayFails() throws Exception {
    given(deadLetterReplayService.replay(10L))
        .willThrow(
            new DeadLetterReplayService.DeadLetterReplayFailedException(
                new DeadLetterReplayResultDto(
                    10L,
                    DeadLetterReplayStatus.FAILED,
                    1,
                    "send failed",
                    null,
                    Instant.parse("2026-03-06T00:10:00Z"))));

    mockMvc
        .perform(post("/ops/dead-letters/10/replay").param("status", "FAILED"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops/dead-letters?status=FAILED"))
        .andExpect(flash().attribute("errorMessage", "Dead letter replay failed."));
  }
}

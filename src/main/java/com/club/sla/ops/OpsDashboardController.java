package com.club.sla.ops;

import com.club.sla.audit.AdminAuditLogService;
import com.club.sla.delivery.OutboundDeliveryJobStatus;
import com.club.sla.delivery.OutboundDeliveryJobType;
import com.club.sla.notify.DeadLetterReplayResultDto;
import com.club.sla.notify.DeadLetterReplayService;
import com.club.sla.notify.DeadLetterReplayStatus;
import com.club.sla.notify.DeadLetterSummaryDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OpsDashboardController {

  private static final int DEAD_LETTER_LIMIT = 100;

  private final OpsOverviewService opsOverviewService;
  private final OpsInstallationSummaryService opsInstallationSummaryService;
  private final OpsDeliveryQueryService opsDeliveryQueryService;
  private final DeadLetterReplayService deadLetterReplayService;
  private final AdminAuditLogService adminAuditLogService;

  public OpsDashboardController(
      OpsOverviewService opsOverviewService,
      OpsInstallationSummaryService opsInstallationSummaryService,
      OpsDeliveryQueryService opsDeliveryQueryService,
      DeadLetterReplayService deadLetterReplayService,
      AdminAuditLogService adminAuditLogService) {
    this.opsOverviewService = opsOverviewService;
    this.opsInstallationSummaryService = opsInstallationSummaryService;
    this.opsDeliveryQueryService = opsDeliveryQueryService;
    this.deadLetterReplayService = deadLetterReplayService;
    this.adminAuditLogService = adminAuditLogService;
  }

  @GetMapping("/ops")
  public String overview(Model model) {
    model.addAttribute("overview", opsOverviewService.loadOverview());
    return "ops-overview";
  }

  @GetMapping("/ops/installations")
  public String installations(Model model) {
    model.addAttribute("installations", opsInstallationSummaryService.listInstallations());
    return "ops-installations";
  }

  @GetMapping("/ops/deliveries")
  public String deliveries(
      @RequestParam(required = false) OutboundDeliveryJobStatus status,
      @RequestParam(required = false) OutboundDeliveryJobType jobType,
      @RequestParam(required = false) Long installationId,
      Model model) {
    model.addAttribute(
        "deliveries",
        opsDeliveryQueryService.listRecentDeliveries(status, jobType, installationId));
    model.addAttribute("selectedStatus", status);
    model.addAttribute("selectedJobType", jobType);
    model.addAttribute("selectedInstallationId", installationId);
    model.addAttribute("availableStatuses", OutboundDeliveryJobStatus.values());
    model.addAttribute("availableJobTypes", OutboundDeliveryJobType.values());
    return "ops-deliveries";
  }

  @GetMapping("/ops/dead-letters")
  public String deadLetters(
      @RequestParam(defaultValue = "PENDING") DeadLetterReplayStatus status, Model model) {
    List<OpsDeadLetterView> deadLetters =
        deadLetterReplayService.list(status, DEAD_LETTER_LIMIT).stream().map(this::toView).toList();
    model.addAttribute("deadLetters", deadLetters);
    model.addAttribute("selectedStatus", status);
    model.addAttribute("availableStatuses", DeadLetterReplayStatus.values());
    return "ops-dead-letters";
  }

  @PostMapping("/ops/dead-letters/{id}/replay")
  public String replayDeadLetter(
      @PathVariable Long id,
      @RequestParam(defaultValue = "PENDING") DeadLetterReplayStatus status,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    try {
      DeadLetterReplayResultDto replayResult = deadLetterReplayService.replay(id);
      adminAuditLogService.record(
          "DEAD_LETTER_REPLAY",
          request.getRequestURI(),
          request.getMethod(),
          200,
          "SUCCESS",
          null,
          null,
          id,
          null);
      redirectAttributes.addFlashAttribute("successMessage", "Dead letter replay enqueued.");
    } catch (DeadLetterReplayService.DeadLetterNotFoundException ex) {
      adminAuditLogService.record(
          "DEAD_LETTER_REPLAY",
          request.getRequestURI(),
          request.getMethod(),
          404,
          "NOT_FOUND",
          null,
          null,
          id,
          ex.getMessage());
      redirectAttributes.addFlashAttribute("errorMessage", "Dead letter not found.");
    } catch (DeadLetterReplayService.DeadLetterAlreadyReplayedException ex) {
      adminAuditLogService.record(
          "DEAD_LETTER_REPLAY",
          request.getRequestURI(),
          request.getMethod(),
          409,
          "CONFLICT",
          null,
          null,
          id,
          ex.getMessage());
      redirectAttributes.addFlashAttribute("errorMessage", "Dead letter already replayed.");
    } catch (DeadLetterReplayService.DeadLetterLegacyMetadataMissingException ex) {
      adminAuditLogService.record(
          "DEAD_LETTER_REPLAY",
          request.getRequestURI(),
          request.getMethod(),
          422,
          "UNPROCESSABLE",
          null,
          null,
          id,
          ex.getMessage());
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Dead letter replay is unavailable for legacy rows.");
    } catch (DeadLetterReplayService.DeadLetterReplayFailedException ex) {
      adminAuditLogService.record(
          "DEAD_LETTER_REPLAY",
          request.getRequestURI(),
          request.getMethod(),
          502,
          "UPSTREAM_FAILED",
          null,
          null,
          id,
          ex.getMessage());
      redirectAttributes.addFlashAttribute("errorMessage", "Dead letter replay failed.");
    }
    return "redirect:/ops/dead-letters?status=" + status.name();
  }

  private OpsDeadLetterView toView(DeadLetterSummaryDto row) {
    return new OpsDeadLetterView(
        row.id(),
        row.reason(),
        row.repoId(),
        row.prNumber(),
        row.stage() == null ? null : row.stage().name(),
        row.replayStatus(),
        row.replayAttempts(),
        row.lastError(),
        row.createdAt(),
        row.replayedAt());
  }
}

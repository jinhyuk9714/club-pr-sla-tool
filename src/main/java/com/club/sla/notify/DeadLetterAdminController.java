package com.club.sla.notify;

import com.club.sla.audit.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dead-letters")
public class DeadLetterAdminController {

  private final DeadLetterReplayService deadLetterReplayService;
  private final AdminAuditLogService adminAuditLogService;

  public DeadLetterAdminController(
      DeadLetterReplayService deadLetterReplayService, AdminAuditLogService adminAuditLogService) {
    this.deadLetterReplayService = deadLetterReplayService;
    this.adminAuditLogService = adminAuditLogService;
  }

  @GetMapping
  public List<DeadLetterSummaryDto> list(
      @RequestParam(defaultValue = "PENDING") DeadLetterReplayStatus status,
      @RequestParam(defaultValue = "50") int limit,
      HttpServletRequest request) {
    List<DeadLetterSummaryDto> rows = deadLetterReplayService.list(status, limit);
    adminAuditLogService.record(
        "DEAD_LETTER_LIST",
        request.getRequestURI(),
        request.getMethod(),
        200,
        "SUCCESS",
        null,
        null,
        null,
        null);
    return rows;
  }

  @PostMapping("/{id}/replay")
  public ResponseEntity<DeadLetterReplayResultDto> replay(
      @PathVariable Long id, HttpServletRequest request) {
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
      return ResponseEntity.ok(replayResult);
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
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
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
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
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
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.replayResult());
    }
  }
}

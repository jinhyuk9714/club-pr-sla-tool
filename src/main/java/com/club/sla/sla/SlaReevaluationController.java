package com.club.sla.sla;

import com.club.sla.audit.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class SlaReevaluationController {

  private final SlaReevaluationService slaReevaluationService;
  private final AdminAuditLogService adminAuditLogService;

  public SlaReevaluationController(
      SlaReevaluationService slaReevaluationService, AdminAuditLogService adminAuditLogService) {
    this.slaReevaluationService = slaReevaluationService;
    this.adminAuditLogService = adminAuditLogService;
  }

  @PostMapping("/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate")
  public ResponseEntity<SlaReevaluationResultDto> reevaluate(
      @PathVariable Long repositoryId, @PathVariable Long prNumber, HttpServletRequest request) {
    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(repositoryId, prNumber);
    if ("NOT_FOUND".equals(result.reason())) {
      adminAuditLogService.record(
          "SLA_REEVALUATE",
          request.getRequestURI(),
          request.getMethod(),
          404,
          "NOT_FOUND",
          repositoryId,
          prNumber,
          null,
          result.reason());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
    adminAuditLogService.record(
        "SLA_REEVALUATE",
        request.getRequestURI(),
        request.getMethod(),
        200,
        "SUCCESS",
        repositoryId,
        prNumber,
        null,
        null);
    return ResponseEntity.ok(result);
  }
}

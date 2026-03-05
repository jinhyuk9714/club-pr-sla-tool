package com.club.sla.sla;

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

  public SlaReevaluationController(SlaReevaluationService slaReevaluationService) {
    this.slaReevaluationService = slaReevaluationService;
  }

  @PostMapping("/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate")
  public ResponseEntity<SlaReevaluationResultDto> reevaluate(
      @PathVariable Long repositoryId, @PathVariable Long prNumber) {
    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(repositoryId, prNumber);
    if ("NOT_FOUND".equals(result.reason())) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
    return ResponseEntity.ok(result);
  }
}

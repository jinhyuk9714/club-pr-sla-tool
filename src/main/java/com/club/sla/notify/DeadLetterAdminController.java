package com.club.sla.notify;

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

  public DeadLetterAdminController(DeadLetterReplayService deadLetterReplayService) {
    this.deadLetterReplayService = deadLetterReplayService;
  }

  @GetMapping
  public List<DeadLetterSummaryDto> list(
      @RequestParam(defaultValue = "PENDING") DeadLetterReplayStatus status,
      @RequestParam(defaultValue = "50") int limit) {
    return deadLetterReplayService.list(status, limit);
  }

  @PostMapping("/{id}/replay")
  public ResponseEntity<DeadLetterReplayResultDto> replay(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(deadLetterReplayService.replay(id));
    } catch (DeadLetterReplayService.DeadLetterNotFoundException ex) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (DeadLetterReplayService.DeadLetterAlreadyReplayedException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } catch (DeadLetterReplayService.DeadLetterLegacyMetadataMissingException ex) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    } catch (DeadLetterReplayService.DeadLetterReplayFailedException ex) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.replayResult());
    }
  }
}

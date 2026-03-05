package com.club.sla.sla;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public class SlaEngine {

  public SlaEvaluation evaluate(
      Instant readyAt,
      Instant firstReviewAt,
      Instant now,
      Set<SlaAction> sentStages,
      boolean fallback36hEnabled) {
    if (readyAt == null || now == null || firstReviewAt != null) {
      return SlaEvaluation.none();
    }

    Duration elapsed = Duration.between(readyAt, now);
    if (elapsed.isNegative()) {
      return SlaEvaluation.none();
    }

    if (elapsed.compareTo(Duration.ofHours(36)) >= 0) {
      if (fallback36hEnabled) {
        return sentStages.contains(SlaAction.FALLBACK_36H)
            ? SlaEvaluation.none()
            : SlaEvaluation.of(SlaAction.FALLBACK_36H);
      }
      return sentStages.contains(SlaAction.ESCALATE_24H)
          ? SlaEvaluation.none()
          : SlaEvaluation.of(SlaAction.ESCALATE_24H);
    }

    if (elapsed.compareTo(Duration.ofHours(24)) >= 0) {
      return sentStages.contains(SlaAction.ESCALATE_24H)
          ? SlaEvaluation.none()
          : SlaEvaluation.of(SlaAction.ESCALATE_24H);
    }

    if (elapsed.compareTo(Duration.ofHours(12)) >= 0) {
      return sentStages.contains(SlaAction.REMIND_12H)
          ? SlaEvaluation.none()
          : SlaEvaluation.of(SlaAction.REMIND_12H);
    }

    return SlaEvaluation.none();
  }
}

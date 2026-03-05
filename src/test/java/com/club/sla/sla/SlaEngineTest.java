package com.club.sla.sla;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class SlaEngineTest {

  private final SlaEngine slaEngine = new SlaEngine();

  @Test
  void returnsNoneBeforeTwelveHours() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds((11 * 60L + 59L) * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(readyAt, null, now, EnumSet.noneOf(SlaAction.class), false);

    assertThat(evaluation.action()).isNull();
  }

  @Test
  void emitsReminderAtTwelveHoursBoundary() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds(12L * 60L * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(readyAt, null, now, EnumSet.noneOf(SlaAction.class), false);

    assertThat(evaluation.action()).isEqualTo(SlaAction.REMIND_12H);
  }

  @Test
  void emitsEscalationAtTwentyFourHoursBoundary() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds(24L * 60L * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(readyAt, null, now, EnumSet.of(SlaAction.REMIND_12H), false);

    assertThat(evaluation.action()).isEqualTo(SlaAction.ESCALATE_24H);
  }

  @Test
  void emitsFallbackAtThirtySixHoursBoundaryWhenEnabled() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds(36L * 60L * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(
            readyAt, null, now, EnumSet.of(SlaAction.REMIND_12H, SlaAction.ESCALATE_24H), true);

    assertThat(evaluation.action()).isEqualTo(SlaAction.FALLBACK_36H);
  }

  @Test
  void doesNotEmitFallbackAtThirtySixHoursWhenDisabled() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds(36L * 60L * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(
            readyAt, null, now, EnumSet.of(SlaAction.REMIND_12H, SlaAction.ESCALATE_24H), false);

    assertThat(evaluation.action()).isNull();
  }

  @Test
  void emitsNoActionWhenAlreadyReviewed() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds(36L * 60L * 60L);
    Instant firstReviewAt = readyAt.plusSeconds(2L * 60L * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(readyAt, firstReviewAt, now, EnumSet.noneOf(SlaAction.class), true);

    assertThat(evaluation.action()).isNull();
  }

  @Test
  void doesNotReemitEscalationWhenAlreadySent() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds(24L * 60L * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(
            readyAt, null, now, EnumSet.of(SlaAction.REMIND_12H, SlaAction.ESCALATE_24H), false);

    assertThat(evaluation.action()).isNull();
  }

  @Test
  void emitsEscalationAtThirtyFiveHoursFiftyNineMinutesWhenFallbackDisabled() {
    Instant readyAt = Instant.parse("2026-03-05T00:00:00Z");
    Instant now = readyAt.plusSeconds((35L * 60L + 59L) * 60L);

    SlaEvaluation evaluation =
        slaEngine.evaluate(readyAt, null, now, EnumSet.of(SlaAction.REMIND_12H), false);

    assertThat(evaluation.action()).isEqualTo(SlaAction.ESCALATE_24H);
  }
}

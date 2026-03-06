package com.club.sla.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.delivery.OutboundDeliveryJobService;
import com.club.sla.sla.SlaAction;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeadLetterReplayServiceTest {

  @Mock private DeadLetterRepository deadLetterRepository;

  @Mock private OutboundDeliveryJobService outboundDeliveryJobService;

  private DeadLetterReplayService deadLetterReplayService;

  @BeforeEach
  void setUp() {
    deadLetterReplayService =
        new DeadLetterReplayService(
            deadLetterRepository,
            outboundDeliveryJobService,
            Clock.fixed(Instant.parse("2026-03-06T01:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void replaysPendingDeadLetterAndMarksAsReplayed() {
    DeadLetterEvent deadLetter = replayableDeadLetter();
    when(deadLetterRepository.findById(1L)).thenReturn(Optional.of(deadLetter));
    when(deadLetterRepository.save(any(DeadLetterEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DeadLetterReplayResultDto result = deadLetterReplayService.replay(1L);

    assertThat(result.replayStatus()).isEqualTo(DeadLetterReplayStatus.REPLAYED);
    assertThat(result.replayedAt()).isNotNull();
    verify(outboundDeliveryJobService, times(1))
        .enqueueDiscordNotification(new NotificationMessage(10L, 20L, SlaAction.ESCALATE_24H));
  }

  @Test
  void throwsWhenDeadLetterIsAlreadyReplayed() {
    DeadLetterEvent deadLetter = replayableDeadLetter();
    deadLetter.markReplaySucceeded(Instant.parse("2026-03-06T00:00:00Z"));
    when(deadLetterRepository.findById(1L)).thenReturn(Optional.of(deadLetter));

    assertThatThrownBy(() -> deadLetterReplayService.replay(1L))
        .isInstanceOf(DeadLetterReplayService.DeadLetterAlreadyReplayedException.class);
  }

  @Test
  void throwsWhenLegacyDeadLetterLacksReplayMetadata() {
    DeadLetterEvent deadLetter =
        new DeadLetterEvent(
            "DISCORD_SEND_FAILED", "[SLA] repo=unknown", Instant.parse("2026-03-06T00:00:00Z"));
    when(deadLetterRepository.findById(2L)).thenReturn(Optional.of(deadLetter));

    assertThatThrownBy(() -> deadLetterReplayService.replay(2L))
        .isInstanceOf(DeadLetterReplayService.DeadLetterLegacyMetadataMissingException.class);
  }

  @Test
  void marksDeadLetterAsFailedWhenReplayEnqueueFails() {
    DeadLetterEvent deadLetter = replayableDeadLetter();
    when(deadLetterRepository.findById(1L)).thenReturn(Optional.of(deadLetter));
    when(deadLetterRepository.save(any(DeadLetterEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    org.mockito.Mockito.doThrow(new RuntimeException("enqueue failed"))
        .when(outboundDeliveryJobService)
        .enqueueDiscordNotification(any(NotificationMessage.class));

    assertThatThrownBy(() -> deadLetterReplayService.replay(1L))
        .isInstanceOf(DeadLetterReplayService.DeadLetterReplayFailedException.class);

    assertThat(deadLetter.getReplayStatus()).isEqualTo(DeadLetterReplayStatus.FAILED);
    assertThat(deadLetter.getReplayAttempts()).isEqualTo(1);
    assertThat(deadLetter.getLastError()).contains("enqueue failed");
  }

  private DeadLetterEvent replayableDeadLetter() {
    return new DeadLetterEvent(
        "DISCORD_SEND_FAILED",
        "[SLA] repo=10 pr=20 stage=ESCALATE_24H",
        Instant.parse("2026-03-06T00:00:00Z"),
        10L,
        20L,
        SlaAction.ESCALATE_24H);
  }
}

package com.club.sla.notify;

import com.club.sla.delivery.OutboundDeliveryJobService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeadLetterReplayService {

  private final DeadLetterRepository deadLetterRepository;
  private final OutboundDeliveryJobService outboundDeliveryJobService;
  private final Clock clock;

  @Autowired
  public DeadLetterReplayService(
      DeadLetterRepository deadLetterRepository,
      OutboundDeliveryJobService outboundDeliveryJobService) {
    this(deadLetterRepository, outboundDeliveryJobService, Clock.systemUTC());
  }

  DeadLetterReplayService(
      DeadLetterRepository deadLetterRepository,
      OutboundDeliveryJobService outboundDeliveryJobService,
      Clock clock) {
    this.deadLetterRepository = deadLetterRepository;
    this.outboundDeliveryJobService = outboundDeliveryJobService;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Transactional(readOnly = true)
  public List<DeadLetterSummaryDto> list(DeadLetterReplayStatus replayStatus, int limit) {
    int safeLimit = limit > 0 ? limit : 50;
    return deadLetterRepository
        .findByReplayStatusOrderByCreatedAtDesc(replayStatus, PageRequest.of(0, safeLimit))
        .stream()
        .map(this::toSummaryDto)
        .toList();
  }

  @Transactional(noRollbackFor = DeadLetterReplayService.DeadLetterReplayFailedException.class)
  public DeadLetterReplayResultDto replay(Long deadLetterId) {
    DeadLetterEvent event =
        deadLetterRepository
            .findById(deadLetterId)
            .orElseThrow(() -> new DeadLetterNotFoundException(deadLetterId));

    if (event.getReplayStatus() == DeadLetterReplayStatus.REPLAYED) {
      throw new DeadLetterAlreadyReplayedException(deadLetterId);
    }
    if (event.getJobType() == null
        && (event.getRepoId() == null || event.getPrNumber() == null || event.getStage() == null)) {
      throw new DeadLetterLegacyMetadataMissingException(deadLetterId);
    }

    try {
      if (event.getJobType() != null) {
        outboundDeliveryJobService.enqueueReplay(event.getJobType(), event.getPayload());
      } else {
        outboundDeliveryJobService.enqueueDiscordNotification(
            new NotificationMessage(event.getRepoId(), event.getPrNumber(), event.getStage()));
      }
      event.markReplaySucceeded(Instant.now(clock));
      DeadLetterEvent saved = deadLetterRepository.save(event);
      return toReplayResultDto(saved);
    } catch (RuntimeException ex) {
      event.markReplayFailed(Instant.now(clock), ex.getMessage());
      DeadLetterEvent saved = deadLetterRepository.save(event);
      throw new DeadLetterReplayFailedException(toReplayResultDto(saved));
    }
  }

  private DeadLetterSummaryDto toSummaryDto(DeadLetterEvent event) {
    return new DeadLetterSummaryDto(
        event.getId(),
        event.getReason(),
        event.getPayload(),
        event.getRepoId(),
        event.getPrNumber(),
        event.getStage(),
        event.getReplayStatus(),
        event.getReplayAttempts(),
        event.getLastError(),
        event.getCreatedAt(),
        event.getReplayedAt(),
        event.getUpdatedAt());
  }

  private DeadLetterReplayResultDto toReplayResultDto(DeadLetterEvent event) {
    return new DeadLetterReplayResultDto(
        event.getId(),
        event.getReplayStatus(),
        event.getReplayAttempts(),
        event.getLastError(),
        event.getReplayedAt(),
        event.getUpdatedAt());
  }

  public static final class DeadLetterNotFoundException extends RuntimeException {
    public DeadLetterNotFoundException(Long deadLetterId) {
      super("dead letter not found: " + deadLetterId);
    }
  }

  public static final class DeadLetterAlreadyReplayedException extends RuntimeException {
    public DeadLetterAlreadyReplayedException(Long deadLetterId) {
      super("dead letter already replayed: " + deadLetterId);
    }
  }

  public static final class DeadLetterLegacyMetadataMissingException extends RuntimeException {
    public DeadLetterLegacyMetadataMissingException(Long deadLetterId) {
      super("dead letter metadata is missing for replay: " + deadLetterId);
    }
  }

  public static final class DeadLetterReplayFailedException extends RuntimeException {
    private final DeadLetterReplayResultDto replayResult;

    public DeadLetterReplayFailedException(DeadLetterReplayResultDto replayResult) {
      super("dead letter replay failed: " + replayResult.id());
      this.replayResult = replayResult;
    }

    public DeadLetterReplayResultDto replayResult() {
      return replayResult;
    }
  }
}

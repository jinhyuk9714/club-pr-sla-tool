package com.club.sla.delivery;

import com.club.sla.github.GithubCheckDeliveryService;
import com.club.sla.metrics.SlaMetrics;
import com.club.sla.notify.DeadLetterEvent;
import com.club.sla.notify.DeadLetterRepository;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.NotificationPort;
import com.club.sla.sla.SchedulerLockService;
import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLog;
import com.club.sla.sla.SlaEventLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OutboundDeliveryJobWorker {

  private static final String LOCK_NAME = "outbound-delivery-jobs";

  private final OutboundDeliveryJobRepository outboundDeliveryJobRepository;
  private final NotificationPort notificationPort;
  private final GithubCheckDeliveryService githubCheckDeliveryService;
  private final DeadLetterRepository deadLetterRepository;
  private final SlaEventLogRepository slaEventLogRepository;
  private final SlaMetrics slaMetrics;
  private final OutboundDeliveryMetrics outboundDeliveryMetrics;
  private final SchedulerLockService schedulerLockService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public OutboundDeliveryJobWorker(
      OutboundDeliveryJobRepository outboundDeliveryJobRepository,
      NotificationPort notificationPort,
      GithubCheckDeliveryService githubCheckDeliveryService,
      DeadLetterRepository deadLetterRepository,
      SlaEventLogRepository slaEventLogRepository,
      SlaMetrics slaMetrics,
      OutboundDeliveryMetrics outboundDeliveryMetrics,
      SchedulerLockService schedulerLockService,
      ObjectMapper objectMapper) {
    this(
        outboundDeliveryJobRepository,
        notificationPort,
        githubCheckDeliveryService,
        deadLetterRepository,
        slaEventLogRepository,
        slaMetrics,
        outboundDeliveryMetrics,
        schedulerLockService,
        objectMapper,
        Clock.systemUTC());
  }

  OutboundDeliveryJobWorker(
      OutboundDeliveryJobRepository outboundDeliveryJobRepository,
      NotificationPort notificationPort,
      GithubCheckDeliveryService githubCheckDeliveryService,
      DeadLetterRepository deadLetterRepository,
      SlaEventLogRepository slaEventLogRepository,
      SlaMetrics slaMetrics,
      OutboundDeliveryMetrics outboundDeliveryMetrics,
      SchedulerLockService schedulerLockService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.outboundDeliveryJobRepository = outboundDeliveryJobRepository;
    this.notificationPort = notificationPort;
    this.githubCheckDeliveryService = githubCheckDeliveryService;
    this.deadLetterRepository = deadLetterRepository;
    this.slaEventLogRepository = slaEventLogRepository;
    this.slaMetrics = slaMetrics;
    this.outboundDeliveryMetrics = outboundDeliveryMetrics;
    this.schedulerLockService = schedulerLockService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${outbound.jobs.interval-ms:60000}")
  public void processPendingJobs() {
    if (!schedulerLockService.tryLock(LOCK_NAME)) {
      return;
    }
    try {
      List<OutboundDeliveryJob> jobs =
          outboundDeliveryJobRepository
              .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                  OutboundDeliveryJobStatus.PENDING, Instant.now(clock));
      for (OutboundDeliveryJob job : jobs) {
        process(job);
      }
    } finally {
      schedulerLockService.unlock(LOCK_NAME);
    }
  }

  private void process(OutboundDeliveryJob job) {
    Instant now = Instant.now(clock);
    job.markProcessing(now);
    outboundDeliveryJobRepository.save(job);

    try {
      if (job.getJobType() == OutboundDeliveryJobType.DISCORD_NOTIFICATION) {
        deliverDiscord(job);
      } else {
        deliverGithubCheck(job);
      }
      job.markSucceeded(now);
      outboundDeliveryJobRepository.save(job);
      outboundDeliveryMetrics.incrementSucceeded(job.getJobType());
    } catch (RuntimeException ex) {
      outboundDeliveryMetrics.incrementAttemptFailure(job.getJobType());
      if (job.getAttemptCount() + 1 >= 3) {
        job.markDead(now, ex.getMessage());
        outboundDeliveryJobRepository.save(job);
        outboundDeliveryMetrics.incrementDead(job.getJobType());
        deadLetterRepository.save(toDeadLetter(job, now));
      } else {
        job.scheduleRetry(now, ex.getMessage(), nextAttemptAt(job.getAttemptCount() + 1, now));
        outboundDeliveryJobRepository.save(job);
      }
    }
  }

  private void deliverDiscord(OutboundDeliveryJob job) {
    DiscordNotificationJobPayload payload =
        deserialize(job.getPayloadJson(), DiscordNotificationJobPayload.class);
    if (slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(
        payload.repositoryId(), payload.prNumber(), payload.stage())) {
      return;
    }
    NotificationMessage notificationMessage =
        new NotificationMessage(payload.repositoryId(), payload.prNumber(), payload.stage());
    notificationPort.send(notificationMessage);
    try {
      slaEventLogRepository.save(
          new SlaEventLog(
              payload.repositoryId(), payload.prNumber(), payload.stage(), Instant.now(clock)));
    } catch (DataIntegrityViolationException ignored) {
      // Concurrent duplicate delivery is treated as already written.
    }
    slaMetrics.incrementNotification(payload.stage());
  }

  private void deliverGithubCheck(OutboundDeliveryJob job) {
    GithubCheckSyncJobPayload payload =
        deserialize(job.getPayloadJson(), GithubCheckSyncJobPayload.class);
    githubCheckDeliveryService.deliver(
        payload.repositoryId(), payload.prNumber(), payload.headSha(), payload.targetState());
  }

  private Instant nextAttemptAt(int nextAttemptNumber, Instant now) {
    return switch (nextAttemptNumber) {
      case 1 -> now.plusSeconds(60);
      case 2 -> now.plusSeconds(5 * 60);
      default -> now.plusSeconds(15 * 60);
    };
  }

  private DeadLetterEvent toDeadLetter(OutboundDeliveryJob job, Instant now) {
    SlaAction stage = null;
    if (job.getJobType() == OutboundDeliveryJobType.DISCORD_NOTIFICATION) {
      stage = deserialize(job.getPayloadJson(), DiscordNotificationJobPayload.class).stage();
    }
    return new DeadLetterEvent(
        job.getJobType().name() + "_DEAD",
        job.getPayloadJson(),
        now,
        job.getRepositoryId(),
        job.getPrNumber(),
        stage,
        job.getJobType());
  }

  private <T> T deserialize(String payloadJson, Class<T> payloadType) {
    try {
      return objectMapper.readValue(payloadJson, payloadType);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to deserialize outbound delivery payload", ex);
    }
  }
}

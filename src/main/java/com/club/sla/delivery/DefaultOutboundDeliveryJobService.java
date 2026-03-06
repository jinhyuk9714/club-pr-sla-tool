package com.club.sla.delivery;

import com.club.sla.github.GithubPullRequestCheckState;
import com.club.sla.installation.InstallationTrackingService;
import com.club.sla.notify.NotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class DefaultOutboundDeliveryJobService implements OutboundDeliveryJobService {

  private static final Set<OutboundDeliveryJobStatus> ACTIVE_JOB_STATUSES =
      EnumSet.of(OutboundDeliveryJobStatus.PENDING, OutboundDeliveryJobStatus.PROCESSING);

  private static final Set<OutboundDeliveryJobStatus> GITHUB_CHECK_DEDUPE_STATUSES =
      EnumSet.of(
          OutboundDeliveryJobStatus.PENDING,
          OutboundDeliveryJobStatus.PROCESSING,
          OutboundDeliveryJobStatus.SUCCEEDED);

  private final OutboundDeliveryJobRepository outboundDeliveryJobRepository;
  private final InstallationTrackingService installationTrackingService;
  private final OutboundDeliveryMetrics outboundDeliveryMetrics;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public DefaultOutboundDeliveryJobService(
      OutboundDeliveryJobRepository outboundDeliveryJobRepository,
      InstallationTrackingService installationTrackingService,
      OutboundDeliveryMetrics outboundDeliveryMetrics,
      ObjectMapper objectMapper) {
    this(
        outboundDeliveryJobRepository,
        installationTrackingService,
        outboundDeliveryMetrics,
        objectMapper,
        Clock.systemUTC());
  }

  DefaultOutboundDeliveryJobService(
      OutboundDeliveryJobRepository outboundDeliveryJobRepository,
      InstallationTrackingService installationTrackingService,
      OutboundDeliveryMetrics outboundDeliveryMetrics,
      ObjectMapper objectMapper,
      Clock clock) {
    this.outboundDeliveryJobRepository = outboundDeliveryJobRepository;
    this.installationTrackingService = installationTrackingService;
    this.outboundDeliveryMetrics = outboundDeliveryMetrics;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  public void enqueueDiscordNotification(NotificationMessage message) {
    enqueue(
        OutboundDeliveryJobType.DISCORD_NOTIFICATION,
        installationTrackingService.findInstallationId(message.repoId()).orElse(null),
        message.repoId(),
        message.prNumber(),
        "discord:%d:%d:%s".formatted(message.repoId(), message.prNumber(), message.stage().name()),
        ACTIVE_JOB_STATUSES,
        serialize(
            new DiscordNotificationJobPayload(
                message.repoId(), message.prNumber(), message.stage())));
  }

  @Override
  public void enqueueGithubCheckSync(
      Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state) {
    if (headSha == null || headSha.isBlank()) {
      return;
    }
    enqueue(
        OutboundDeliveryJobType.GITHUB_CHECK_SYNC,
        installationTrackingService.findInstallationId(repositoryId).orElse(null),
        repositoryId,
        prNumber,
        "github-check:%d:%d:%s:%s".formatted(repositoryId, prNumber, headSha, state.name()),
        GITHUB_CHECK_DEDUPE_STATUSES,
        serialize(new GithubCheckSyncJobPayload(repositoryId, prNumber, headSha, state)));
  }

  @Override
  public void enqueueReplay(OutboundDeliveryJobType jobType, String payloadJson) {
    switch (jobType) {
      case DISCORD_NOTIFICATION -> {
        DiscordNotificationJobPayload payload =
            deserialize(payloadJson, DiscordNotificationJobPayload.class);
        enqueueDiscordNotification(
            new NotificationMessage(payload.repositoryId(), payload.prNumber(), payload.stage()));
      }
      case GITHUB_CHECK_SYNC -> {
        GithubCheckSyncJobPayload payload =
            deserialize(payloadJson, GithubCheckSyncJobPayload.class);
        enqueueGithubCheckSync(
            payload.repositoryId(), payload.prNumber(), payload.headSha(), payload.targetState());
      }
    }
  }

  private void enqueue(
      OutboundDeliveryJobType jobType,
      Long installationId,
      Long repositoryId,
      Long prNumber,
      String uniqueKey,
      Set<OutboundDeliveryJobStatus> dedupeStatuses,
      String payloadJson) {
    if (outboundDeliveryJobRepository
        .findByUniqueKeyAndStatusIn(uniqueKey, dedupeStatuses)
        .isPresent()) {
      return;
    }
    try {
      outboundDeliveryJobRepository.save(
          new OutboundDeliveryJob(
              jobType,
              uniqueKey,
              installationId,
              repositoryId,
              prNumber,
              payloadJson,
              Instant.now(clock)));
      outboundDeliveryMetrics.incrementCreated(jobType);
    } catch (DataIntegrityViolationException ignored) {
      // Duplicate active job is treated as already enqueued.
    }
  }

  private String serialize(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize outbound delivery payload", ex);
    }
  }

  private <T> T deserialize(String payloadJson, Class<T> payloadType) {
    try {
      return objectMapper.readValue(payloadJson, payloadType);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to deserialize outbound delivery payload", ex);
    }
  }
}

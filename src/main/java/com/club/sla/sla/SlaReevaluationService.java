package com.club.sla.sla;

import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.SlaNotificationService;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaReevaluationService {

  private final PullRequestStateRepository pullRequestStateRepository;
  private final SlaEventLogRepository slaEventLogRepository;
  private final SlaNotificationService slaNotificationService;
  private final SlaEngine slaEngine = new SlaEngine();
  private final Clock clock;

  @Value("${sla.policy.fallback-36h-enabled:false}")
  private boolean fallback36hEnabled;

  @Autowired
  public SlaReevaluationService(
      PullRequestStateRepository pullRequestStateRepository,
      SlaEventLogRepository slaEventLogRepository,
      SlaNotificationService slaNotificationService) {
    this(
        pullRequestStateRepository,
        slaEventLogRepository,
        slaNotificationService,
        Clock.systemUTC());
  }

  SlaReevaluationService(
      PullRequestStateRepository pullRequestStateRepository,
      SlaEventLogRepository slaEventLogRepository,
      SlaNotificationService slaNotificationService,
      Clock clock) {
    this.pullRequestStateRepository = pullRequestStateRepository;
    this.slaEventLogRepository = slaEventLogRepository;
    this.slaNotificationService = slaNotificationService;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Transactional
  public SlaReevaluationResultDto reevaluate(Long repositoryId, Long prNumber) {
    Instant now = Instant.now(clock);
    Optional<PullRequestState> maybeState =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(repositoryId, prNumber);
    if (maybeState.isEmpty()) {
      return new SlaReevaluationResultDto(false, null, "NOT_FOUND", now);
    }

    PullRequestState state = maybeState.get();
    if (state.getFirstReviewAt() != null) {
      return new SlaReevaluationResultDto(false, null, "ALREADY_REVIEWED", now);
    }

    Set<SlaAction> sentStages = loadSentStages(repositoryId, prNumber);
    SlaEvaluation evaluation =
        slaEngine.evaluate(
            state.getReadyAt(), state.getFirstReviewAt(), now, sentStages, fallback36hEnabled);
    if (!evaluation.hasAction()) {
      SlaAction expectedStage = expectedStage(state.getReadyAt(), now, fallback36hEnabled);
      String reason =
          expectedStage != null && sentStages.contains(expectedStage)
              ? "ALREADY_SENT"
              : "NO_ACTION";
      return new SlaReevaluationResultDto(false, null, reason, now);
    }

    slaNotificationService.dispatch(
        new NotificationMessage(repositoryId, prNumber, evaluation.action()));
    return new SlaReevaluationResultDto(true, evaluation.action(), "ACTION_DISPATCHED", now);
  }

  private Set<SlaAction> loadSentStages(Long repositoryId, Long prNumber) {
    List<SlaEventLog> logs = slaEventLogRepository.findByRepoIdAndPrNumber(repositoryId, prNumber);
    Set<SlaAction> stages = EnumSet.noneOf(SlaAction.class);
    for (SlaEventLog log : logs) {
      stages.add(log.getStage());
    }
    return stages;
  }

  private SlaAction expectedStage(Instant readyAt, Instant now, boolean fallbackEnabled) {
    if (readyAt == null || now == null) {
      return null;
    }
    Duration elapsed = Duration.between(readyAt, now);
    if (elapsed.isNegative()) {
      return null;
    }
    if (elapsed.compareTo(Duration.ofHours(36)) >= 0) {
      return fallbackEnabled ? SlaAction.FALLBACK_36H : SlaAction.ESCALATE_24H;
    }
    if (elapsed.compareTo(Duration.ofHours(24)) >= 0) {
      return SlaAction.ESCALATE_24H;
    }
    if (elapsed.compareTo(Duration.ofHours(12)) >= 0) {
      return SlaAction.REMIND_12H;
    }
    return null;
  }
}

package com.club.sla.sla;

import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.SlaNotificationService;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SlaScannerJob {

  private static final String LOCK_NAME = "sla-scan";

  private final SchedulerLockService schedulerLockService;
  private final PullRequestStateRepository pullRequestStateRepository;
  private final SlaEventLogRepository slaEventLogRepository;
  private final SlaNotificationService slaNotificationService;
  private final SlaEngine slaEngine = new SlaEngine();
  private final Clock clock = Clock.systemUTC();

  @Value("${sla.policy.fallback-36h-enabled:false}")
  private boolean fallback36hEnabled;

  public SlaScannerJob(
      SchedulerLockService schedulerLockService,
      PullRequestStateRepository pullRequestStateRepository,
      SlaEventLogRepository slaEventLogRepository,
      SlaNotificationService slaNotificationService) {
    this.schedulerLockService = schedulerLockService;
    this.pullRequestStateRepository = pullRequestStateRepository;
    this.slaEventLogRepository = slaEventLogRepository;
    this.slaNotificationService = slaNotificationService;
  }

  @Scheduled(
      fixedDelayString = "${sla.scanner.interval-ms:300000}",
      initialDelayString = "${sla.scanner.initial-delay-ms:300000}")
  public void scan() {
    if (!schedulerLockService.tryLock(LOCK_NAME)) {
      return;
    }

    try {
      List<PullRequestState> trackablePullRequests =
          pullRequestStateRepository.findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
              PullRequestStatus.READY);
      Instant now = Instant.now(clock);
      for (PullRequestState pullRequest : trackablePullRequests) {
        processPullRequest(pullRequest, now);
      }
    } finally {
      schedulerLockService.unlock(LOCK_NAME);
    }
  }

  private void processPullRequest(PullRequestState pullRequest, Instant now) {
    try {
      Set<SlaAction> sentStages =
          loadSentStages(pullRequest.getRepositoryId(), pullRequest.getPrNumber());
      SlaEvaluation evaluation =
          slaEngine.evaluate(
              pullRequest.getReadyAt(),
              pullRequest.getFirstReviewAt(),
              now,
              sentStages,
              fallback36hEnabled);
      if (!evaluation.hasAction()) {
        return;
      }
      slaNotificationService.dispatch(
          new NotificationMessage(
              pullRequest.getRepositoryId(), pullRequest.getPrNumber(), evaluation.action()));
    } catch (RuntimeException ignored) {
      // Individual PR failures should not block other PR evaluations.
    }
  }

  private Set<SlaAction> loadSentStages(Long repositoryId, Long prNumber) {
    List<SlaEventLog> eventLogs =
        slaEventLogRepository.findByRepoIdAndPrNumber(repositoryId, prNumber);
    Set<SlaAction> stages = EnumSet.noneOf(SlaAction.class);
    for (SlaEventLog eventLog : eventLogs) {
      stages.add(eventLog.getStage());
    }
    return stages;
  }
}

package com.club.sla.sla;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.SlaNotificationService;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SlaScannerJobTest {

  @Mock private SchedulerLockService schedulerLockService;
  @Mock private PullRequestStateRepository pullRequestStateRepository;
  @Mock private SlaEventLogRepository slaEventLogRepository;
  @Mock private SlaNotificationService slaNotificationService;

  @InjectMocks private SlaScannerJob slaScannerJob;

  @Test
  void skipsScanWhenLockIsNotAcquired() {
    when(schedulerLockService.tryLock("sla-scan")).thenReturn(false);

    slaScannerJob.scan();

    verify(pullRequestStateRepository, never())
        .findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(PullRequestStatus.READY);
    verify(slaNotificationService, never()).dispatch(any(NotificationMessage.class));
    verify(schedulerLockService, never()).unlock("sla-scan");
  }

  @Test
  void dispatchesNotificationWhenSlaThresholdIsReached() {
    when(schedulerLockService.tryLock("sla-scan")).thenReturn(true);

    PullRequestState pullRequest =
        readyPullRequest(1L, 101L, Instant.now().minus(Duration.ofHours(13)));
    when(pullRequestStateRepository.findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
            PullRequestStatus.READY))
        .thenReturn(List.of(pullRequest));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(1L, 101L)).thenReturn(List.of());

    slaScannerJob.scan();

    verify(slaNotificationService, times(1))
        .dispatch(eq(new NotificationMessage(1L, 101L, SlaAction.REMIND_12H)));
    verify(schedulerLockService, times(1)).unlock("sla-scan");
  }

  @Test
  void doesNotDispatchWhenStageAlreadySent() {
    when(schedulerLockService.tryLock("sla-scan")).thenReturn(true);

    PullRequestState pullRequest =
        readyPullRequest(2L, 201L, Instant.now().minus(Duration.ofHours(13)));
    when(pullRequestStateRepository.findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
            PullRequestStatus.READY))
        .thenReturn(List.of(pullRequest));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(2L, 201L))
        .thenReturn(List.of(new SlaEventLog(2L, 201L, SlaAction.REMIND_12H, Instant.now())));

    slaScannerJob.scan();

    verify(slaNotificationService, never()).dispatch(any(NotificationMessage.class));
    verify(schedulerLockService, times(1)).unlock("sla-scan");
  }

  @Test
  void doesNotDispatchWhenFirstReviewAlreadyExists() {
    when(schedulerLockService.tryLock("sla-scan")).thenReturn(true);

    PullRequestState pullRequest =
        readyPullRequest(3L, 301L, Instant.now().minus(Duration.ofHours(25)));
    pullRequest.setFirstReviewAt(Instant.now().minus(Duration.ofHours(1)));
    when(pullRequestStateRepository.findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
            PullRequestStatus.READY))
        .thenReturn(List.of(pullRequest));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(3L, 301L)).thenReturn(List.of());

    slaScannerJob.scan();

    verify(slaNotificationService, never()).dispatch(any(NotificationMessage.class));
    verify(schedulerLockService, times(1)).unlock("sla-scan");
  }

  @Test
  void continuesScanningWhenSinglePullRequestDispatchFails() {
    when(schedulerLockService.tryLock("sla-scan")).thenReturn(true);

    PullRequestState first = readyPullRequest(4L, 401L, Instant.now().minus(Duration.ofHours(13)));
    PullRequestState second = readyPullRequest(4L, 402L, Instant.now().minus(Duration.ofHours(25)));
    when(pullRequestStateRepository.findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
            PullRequestStatus.READY))
        .thenReturn(List.of(first, second));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(4L, 401L)).thenReturn(List.of());
    when(slaEventLogRepository.findByRepoIdAndPrNumber(4L, 402L)).thenReturn(List.of());
    doThrow(new RuntimeException("dispatch failed"))
        .doNothing()
        .when(slaNotificationService)
        .dispatch(any(NotificationMessage.class));

    slaScannerJob.scan();

    verify(slaNotificationService, times(2)).dispatch(any(NotificationMessage.class));
    verify(schedulerLockService, times(1)).unlock("sla-scan");
  }

  @Test
  void unlocksWhenUnexpectedFailureOccurs() {
    when(schedulerLockService.tryLock("sla-scan")).thenReturn(true);
    when(pullRequestStateRepository.findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
            PullRequestStatus.READY))
        .thenThrow(new RuntimeException("query failed"));

    try {
      slaScannerJob.scan();
    } catch (RuntimeException ignored) {
      // Expected for this scenario.
    }

    verify(schedulerLockService, times(1)).unlock("sla-scan");
  }

  private PullRequestState readyPullRequest(Long repoId, Long prNumber, Instant readyAt) {
    PullRequestState pullRequestState = new PullRequestState(repoId, prNumber, "author");
    pullRequestState.setStatus(PullRequestStatus.READY);
    pullRequestState.setReadyAt(readyAt);
    ReflectionTestUtils.setField(pullRequestState, "firstReviewAt", null);
    return pullRequestState;
  }
}

package com.club.sla.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.installation.InstallationTrackingService;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.SlaNotificationService;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SlaReevaluationServiceTest {

  @Mock private PullRequestStateRepository pullRequestStateRepository;
  @Mock private SlaEventLogRepository slaEventLogRepository;
  @Mock private SlaNotificationService slaNotificationService;
  @Mock private InstallationTrackingService installationTrackingService;

  private SlaReevaluationService slaReevaluationService;

  @BeforeEach
  void setUp() {
    slaReevaluationService =
        new SlaReevaluationService(
            pullRequestStateRepository,
            slaEventLogRepository,
            slaNotificationService,
            installationTrackingService,
            Clock.fixed(Instant.parse("2026-03-06T01:00:00Z"), ZoneOffset.UTC));
    ReflectionTestUtils.setField(slaReevaluationService, "fallback36hEnabled", false);
  }

  @Test
  void returnsNotFoundWhenPullRequestStateDoesNotExist() {
    when(pullRequestStateRepository.findByRepositoryIdAndPrNumber(1L, 100L))
        .thenReturn(Optional.empty());

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(1L, 100L);

    assertThat(result.actionDispatched()).isFalse();
    assertThat(result.reason()).isEqualTo("NOT_FOUND");
  }

  @Test
  void returnsAlreadyReviewedWhenFirstReviewExists() {
    PullRequestState state = readyPullRequest(1L, 101L, Instant.parse("2026-03-05T10:00:00Z"));
    state.setFirstReviewAt(Instant.parse("2026-03-05T12:00:00Z"));
    when(pullRequestStateRepository.findByRepositoryIdAndPrNumber(1L, 101L))
        .thenReturn(Optional.of(state));
    when(installationTrackingService.isRepositoryConfigured(1L)).thenReturn(true);

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(1L, 101L);

    assertThat(result.actionDispatched()).isFalse();
    assertThat(result.reason()).isEqualTo("ALREADY_REVIEWED");
  }

  @Test
  void returnsNoActionWhenThresholdIsNotReached() {
    PullRequestState state = readyPullRequest(1L, 102L, Instant.parse("2026-03-05T16:00:00Z"));
    when(pullRequestStateRepository.findByRepositoryIdAndPrNumber(1L, 102L))
        .thenReturn(Optional.of(state));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(1L, 102L)).thenReturn(List.of());
    when(installationTrackingService.isRepositoryConfigured(1L)).thenReturn(true);

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(1L, 102L);

    assertThat(result.actionDispatched()).isFalse();
    assertThat(result.reason()).isEqualTo("NO_ACTION");
  }

  @Test
  void returnsAlreadySentWhenStageAlreadyLogged() {
    PullRequestState state = readyPullRequest(1L, 103L, Instant.parse("2026-03-05T10:00:00Z"));
    when(pullRequestStateRepository.findByRepositoryIdAndPrNumber(1L, 103L))
        .thenReturn(Optional.of(state));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(1L, 103L))
        .thenReturn(
            List.of(
                new SlaEventLog(
                    1L, 103L, SlaAction.REMIND_12H, Instant.parse("2026-03-05T22:00:00Z"))));
    when(installationTrackingService.isRepositoryConfigured(1L)).thenReturn(true);

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(1L, 103L);

    assertThat(result.actionDispatched()).isFalse();
    assertThat(result.reason()).isEqualTo("ALREADY_SENT");
  }

  @Test
  void dispatchesActionWhenSlaActionExists() {
    PullRequestState state = readyPullRequest(1L, 104L, Instant.parse("2026-03-05T10:00:00Z"));
    when(pullRequestStateRepository.findByRepositoryIdAndPrNumber(1L, 104L))
        .thenReturn(Optional.of(state));
    when(slaEventLogRepository.findByRepoIdAndPrNumber(1L, 104L)).thenReturn(List.of());
    when(installationTrackingService.isRepositoryConfigured(1L)).thenReturn(true);

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(1L, 104L);

    assertThat(result.actionDispatched()).isTrue();
    assertThat(result.action()).isEqualTo(SlaAction.REMIND_12H);
    assertThat(result.reason()).isEqualTo("ACTION_DISPATCHED");
    verify(slaNotificationService, times(1))
        .dispatch(new NotificationMessage(1L, 104L, SlaAction.REMIND_12H));
  }

  @Test
  void returnsRepositoryNotTrackedWhenInstallationIsNotConfigured() {
    PullRequestState state = readyPullRequest(1L, 105L, Instant.parse("2026-03-05T10:00:00Z"));
    when(pullRequestStateRepository.findByRepositoryIdAndPrNumber(1L, 105L))
        .thenReturn(Optional.of(state));
    when(installationTrackingService.isRepositoryConfigured(1L)).thenReturn(false);

    SlaReevaluationResultDto result = slaReevaluationService.reevaluate(1L, 105L);

    assertThat(result.actionDispatched()).isFalse();
    assertThat(result.reason()).isEqualTo("REPOSITORY_NOT_TRACKED");
  }

  private PullRequestState readyPullRequest(Long repoId, Long prNumber, Instant readyAt) {
    PullRequestState state = new PullRequestState(repoId, prNumber, "alice");
    state.setStatus(PullRequestStatus.READY);
    state.setReadyAt(readyAt);
    return state;
  }
}

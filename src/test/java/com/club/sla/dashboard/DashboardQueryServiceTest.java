package com.club.sla.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceTest {

  @Mock private PullRequestStateRepository pullRequestStateRepository;

  @Test
  void classifiesDashboardBucketsWithBoundaries() {
    Instant now = Instant.parse("2026-03-05T12:00:00Z");
    DashboardQueryService dashboardQueryService =
        new DashboardQueryService(pullRequestStateRepository, Clock.fixed(now, ZoneOffset.UTC));

    PullRequestState onTrack = readyState(1L, now.minusSeconds(11 * 3600 + 59 * 60), null);
    PullRequestState atRiskAt12h = readyState(2L, now.minusSeconds(12 * 3600), null);
    PullRequestState atRiskAt2359 = readyState(3L, now.minusSeconds(23 * 3600 + 59 * 60), null);
    PullRequestState breachedAt24h = readyState(4L, now.minusSeconds(24 * 3600), null);
    PullRequestState reviewed = readyState(5L, now.minusSeconds(72 * 3600), now.minusSeconds(10));
    PullRequestState nullReadyAt = readyState(6L, null, null);

    when(pullRequestStateRepository.findByRepositoryIdAndReadyAtIsNotNull(9001L))
        .thenReturn(
            List.of(onTrack, atRiskAt12h, atRiskAt2359, breachedAt24h, reviewed, nullReadyAt));

    DashboardSummaryDto summary = dashboardQueryService.fetch(9001L);

    assertThat(summary.onTrack()).isEqualTo(2);
    assertThat(summary.atRisk()).isEqualTo(2);
    assertThat(summary.breached()).isEqualTo(1);
    verify(pullRequestStateRepository).findByRepositoryIdAndReadyAtIsNotNull(9001L);
  }

  private PullRequestState readyState(Long prNumber, Instant readyAt, Instant firstReviewAt) {
    PullRequestState pullRequestState = new PullRequestState(9001L, prNumber, "alice");
    pullRequestState.setStatus(PullRequestStatus.READY);
    pullRequestState.setReadyAt(readyAt);
    pullRequestState.setFirstReviewAt(firstReviewAt);
    return pullRequestState;
  }
}

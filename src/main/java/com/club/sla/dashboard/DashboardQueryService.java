package com.club.sla.dashboard;

import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardQueryService {

  private final PullRequestStateRepository pullRequestStateRepository;
  private final Clock clock;

  @Autowired
  public DashboardQueryService(PullRequestStateRepository pullRequestStateRepository) {
    this(pullRequestStateRepository, Clock.systemUTC());
  }

  DashboardQueryService(PullRequestStateRepository pullRequestStateRepository, Clock clock) {
    this.pullRequestStateRepository = pullRequestStateRepository;
    this.clock = clock;
  }

  public DashboardSummaryDto fetch(Long repositoryId) {
    List<PullRequestState> states =
        pullRequestStateRepository.findByRepositoryIdAndReadyAtIsNotNull(repositoryId);
    long onTrack = 0;
    long atRisk = 0;
    long breached = 0;

    Instant now = Instant.now(clock);
    for (PullRequestState state : states) {
      if (state.getReadyAt() == null) {
        continue;
      }
      if (state.getFirstReviewAt() != null) {
        onTrack++;
        continue;
      }

      Duration elapsed = Duration.between(state.getReadyAt(), now);
      if (elapsed.isNegative()) {
        onTrack++;
      } else if (elapsed.compareTo(Duration.ofHours(24)) >= 0) {
        breached++;
      } else if (elapsed.compareTo(Duration.ofHours(12)) >= 0) {
        atRisk++;
      } else {
        onTrack++;
      }
    }
    return new DashboardSummaryDto(onTrack, atRisk, breached);
  }
}

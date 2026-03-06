package com.club.sla.pr;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PullRequestStateService {

  private final PullRequestStateRepository pullRequestStateRepository;
  private final Clock clock = Clock.systemUTC();

  public PullRequestStateService(PullRequestStateRepository pullRequestStateRepository) {
    this.pullRequestStateRepository = pullRequestStateRepository;
  }

  @Transactional
  public PullRequestState onReadyForReview(Long repositoryId, Long prNumber, String authorLogin) {
    return onReadyForReview(repositoryId, prNumber, authorLogin, null);
  }

  @Transactional
  public PullRequestState onReadyForReview(
      Long repositoryId, Long prNumber, String authorLogin, String headSha) {
    PullRequestState state = getOrCreate(repositoryId, prNumber, authorLogin);
    state.setAuthorLogin(authorLogin);
    state.setStatus(PullRequestStatus.READY);
    state.setReadyAt(Instant.now(clock));
    state.setHeadSha(headSha);
    return pullRequestStateRepository.save(state);
  }

  @Transactional
  public PullRequestState onDraft(Long repositoryId, Long prNumber) {
    PullRequestState state = getOrCreate(repositoryId, prNumber, null);
    state.setStatus(PullRequestStatus.DRAFT);
    state.setReadyAt(null);
    return pullRequestStateRepository.save(state);
  }

  @Transactional
  public PullRequestState onFirstReview(Long repositoryId, Long prNumber, String reviewerLogin) {
    return onFirstReview(repositoryId, prNumber, reviewerLogin, null);
  }

  @Transactional
  public PullRequestState onFirstReview(
      Long repositoryId, Long prNumber, String reviewerLogin, String headSha) {
    PullRequestState state = getOrCreate(repositoryId, prNumber, null);
    if (state.getFirstReviewAt() == null) {
      state.setFirstReviewAt(Instant.now(clock));
      state.setFirstReviewerLogin(reviewerLogin);
    }
    if (headSha != null && !headSha.isBlank()) {
      state.setHeadSha(headSha);
    }
    state.setStatus(PullRequestStatus.REVIEWED);
    return pullRequestStateRepository.save(state);
  }

  private PullRequestState getOrCreate(Long repositoryId, Long prNumber, String authorLogin) {
    return pullRequestStateRepository
        .findByRepositoryIdAndPrNumber(repositoryId, prNumber)
        .orElseGet(() -> new PullRequestState(repositoryId, prNumber, authorLogin));
  }
}

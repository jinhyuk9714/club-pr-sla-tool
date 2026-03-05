package com.club.sla.webhook;

import com.club.sla.pr.PullRequestStateService;
import com.club.sla.webhook.GithubEventMapper.ConvertedToDraftEvent;
import com.club.sla.webhook.GithubEventMapper.ReadyForReviewEvent;
import com.club.sla.webhook.GithubEventMapper.ReviewSubmittedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIngestionService implements WebhookIngestionPort {

  private final DeliveryIngestionService deliveryIngestionService;
  private final PullRequestStateService pullRequestStateService;
  private final GithubEventMapper githubEventMapper;

  public WebhookIngestionService(
      DeliveryIngestionService deliveryIngestionService,
      PullRequestStateService pullRequestStateService,
      GithubEventMapper githubEventMapper) {
    this.deliveryIngestionService = deliveryIngestionService;
    this.pullRequestStateService = pullRequestStateService;
    this.githubEventMapper = githubEventMapper;
  }

  @Override
  @Transactional
  public void ingest(String deliveryId, String eventType, String payload) {
    boolean recorded = deliveryIngestionService.record(deliveryId, eventType, payload);
    if (!recorded) {
      return;
    }

    githubEventMapper
        .map(eventType, payload)
        .ifPresent(
            event -> {
              if (event instanceof ReadyForReviewEvent readyForReviewEvent) {
                pullRequestStateService.onReadyForReview(
                    readyForReviewEvent.repositoryId(),
                    readyForReviewEvent.prNumber(),
                    readyForReviewEvent.authorLogin());
                return;
              }
              if (event instanceof ConvertedToDraftEvent convertedToDraftEvent) {
                pullRequestStateService.onDraft(
                    convertedToDraftEvent.repositoryId(), convertedToDraftEvent.prNumber());
                return;
              }
              if (event instanceof ReviewSubmittedEvent reviewSubmittedEvent) {
                pullRequestStateService.onFirstReview(
                    reviewSubmittedEvent.repositoryId(),
                    reviewSubmittedEvent.prNumber(),
                    reviewSubmittedEvent.reviewerLogin());
              }
            });
  }
}

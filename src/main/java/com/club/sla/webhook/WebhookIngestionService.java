package com.club.sla.webhook;

import com.club.sla.github.GithubPullRequestCheckService;
import com.club.sla.github.GithubPullRequestCheckState;
import com.club.sla.installation.GithubInstallationRepositoryEntryUpsertCommand;
import com.club.sla.installation.GithubInstallationStatus;
import com.club.sla.installation.InstallationRegistryService;
import com.club.sla.installation.InstallationTrackingService;
import com.club.sla.pr.PullRequestStateService;
import com.club.sla.webhook.GithubEventMapper.ConvertedToDraftEvent;
import com.club.sla.webhook.GithubEventMapper.InstallationCreatedEvent;
import com.club.sla.webhook.GithubEventMapper.InstallationRemovedEvent;
import com.club.sla.webhook.GithubEventMapper.InstallationRepositoriesChangedEvent;
import com.club.sla.webhook.GithubEventMapper.ReadyForReviewEvent;
import com.club.sla.webhook.GithubEventMapper.ReviewSubmittedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIngestionService implements WebhookIngestionPort {

  private final DeliveryIngestionService deliveryIngestionService;
  private final InstallationRegistryService installationRegistryService;
  private final InstallationTrackingService installationTrackingService;
  private final PullRequestStateService pullRequestStateService;
  private final GithubPullRequestCheckService githubPullRequestCheckService;
  private final GithubEventMapper githubEventMapper;

  public WebhookIngestionService(
      DeliveryIngestionService deliveryIngestionService,
      InstallationRegistryService installationRegistryService,
      InstallationTrackingService installationTrackingService,
      PullRequestStateService pullRequestStateService,
      GithubPullRequestCheckService githubPullRequestCheckService,
      GithubEventMapper githubEventMapper) {
    this.deliveryIngestionService = deliveryIngestionService;
    this.installationRegistryService = installationRegistryService;
    this.installationTrackingService = installationTrackingService;
    this.pullRequestStateService = pullRequestStateService;
    this.githubPullRequestCheckService = githubPullRequestCheckService;
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
              if (event instanceof InstallationCreatedEvent installationCreatedEvent) {
                installationRegistryService.upsertInstallation(
                    new com.club.sla.installation.GithubInstallationUpsertCommand(
                        installationCreatedEvent.installationId(),
                        installationCreatedEvent.accountId(),
                        installationCreatedEvent.accountLogin(),
                        installationCreatedEvent.accountType(),
                        installationCreatedEvent.installedAt()));
                installationRegistryService.syncRepositories(
                    installationCreatedEvent.installationId(),
                    installationCreatedEvent.repositories().stream()
                        .map(
                            repository ->
                                new GithubInstallationRepositoryEntryUpsertCommand(
                                    repository.repositoryId(),
                                    repository.repositoryName(),
                                    repository.repositoryFullName()))
                        .toList());
                return;
              }
              if (event instanceof InstallationRemovedEvent installationRemovedEvent) {
                installationRegistryService.markInstallationStatus(
                    installationRemovedEvent.installationId(), GithubInstallationStatus.REMOVED);
                return;
              }
              if (event
                  instanceof
                  InstallationRepositoriesChangedEvent installationRepositoriesChangedEvent) {
                installationRegistryService.upsertRepositories(
                    installationRepositoriesChangedEvent.installationId(),
                    installationRepositoriesChangedEvent.repositoriesAdded().stream()
                        .map(
                            repository ->
                                new GithubInstallationRepositoryEntryUpsertCommand(
                                    repository.repositoryId(),
                                    repository.repositoryName(),
                                    repository.repositoryFullName()))
                        .toList());
                installationRegistryService.deactivateRepositories(
                    installationRepositoriesChangedEvent.installationId(),
                    installationRepositoriesChangedEvent.repositoriesRemoved());
                return;
              }
              if (event instanceof ReadyForReviewEvent readyForReviewEvent) {
                if (!installationTrackingService.isRepositoryActive(
                    readyForReviewEvent.repositoryId())) {
                  return;
                }
                if (!installationTrackingService.isRepositoryConfigured(
                    readyForReviewEvent.repositoryId())) {
                  githubPullRequestCheckService.syncStatus(
                      readyForReviewEvent.repositoryId(),
                      readyForReviewEvent.prNumber(),
                      readyForReviewEvent.headSha(),
                      GithubPullRequestCheckState.APP_NOT_CONFIGURED);
                  return;
                }
                pullRequestStateService.onReadyForReview(
                    readyForReviewEvent.repositoryId(),
                    readyForReviewEvent.prNumber(),
                    readyForReviewEvent.authorLogin(),
                    readyForReviewEvent.headSha());
                githubPullRequestCheckService.syncStatus(
                    readyForReviewEvent.repositoryId(),
                    readyForReviewEvent.prNumber(),
                    readyForReviewEvent.headSha(),
                    GithubPullRequestCheckState.ON_TRACK);
                return;
              }
              if (event instanceof ConvertedToDraftEvent convertedToDraftEvent) {
                if (!installationTrackingService.isRepositoryActive(
                    convertedToDraftEvent.repositoryId())) {
                  return;
                }
                pullRequestStateService.onDraft(
                    convertedToDraftEvent.repositoryId(), convertedToDraftEvent.prNumber());
                return;
              }
              if (event instanceof ReviewSubmittedEvent reviewSubmittedEvent) {
                if (!installationTrackingService.isRepositoryConfigured(
                    reviewSubmittedEvent.repositoryId())) {
                  return;
                }
                pullRequestStateService.onFirstReview(
                    reviewSubmittedEvent.repositoryId(),
                    reviewSubmittedEvent.prNumber(),
                    reviewSubmittedEvent.reviewerLogin(),
                    reviewSubmittedEvent.headSha());
                githubPullRequestCheckService.syncStatus(
                    reviewSubmittedEvent.repositoryId(),
                    reviewSubmittedEvent.prNumber(),
                    reviewSubmittedEvent.headSha(),
                    GithubPullRequestCheckState.REVIEWED);
              }
            });
  }
}

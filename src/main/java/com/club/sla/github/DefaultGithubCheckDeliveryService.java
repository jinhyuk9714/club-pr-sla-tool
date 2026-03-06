package com.club.sla.github;

import com.club.sla.installation.InstallationTrackingService;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultGithubCheckDeliveryService implements GithubCheckDeliveryService {

  private static final String CHECK_NAME = "Club PR SLA";

  private final GithubAppClient githubAppClient;
  private final InstallationTrackingService installationTrackingService;
  private final Clock clock;

  @Autowired
  public DefaultGithubCheckDeliveryService(
      GithubAppClient githubAppClient, InstallationTrackingService installationTrackingService) {
    this(githubAppClient, installationTrackingService, Clock.systemUTC());
  }

  DefaultGithubCheckDeliveryService(
      GithubAppClient githubAppClient,
      InstallationTrackingService installationTrackingService,
      Clock clock) {
    this.githubAppClient = githubAppClient;
    this.installationTrackingService = installationTrackingService;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void deliver(
      Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state) {
    if (headSha == null || headSha.isBlank()) {
      throw new IllegalStateException("GitHub check delivery requires a head SHA");
    }
    Long installationId =
        installationTrackingService
            .findInstallationId(repositoryId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No installation found for repository " + repositoryId));
    String repositoryFullName =
        installationTrackingService
            .findRepositoryFullName(repositoryId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No repository full name found for repository " + repositoryId));
    githubAppClient.upsertCheckRun(
        installationId,
        repositoryFullName,
        toCommand(prNumber, headSha, state, Instant.now(clock)));
  }

  private GithubCheckRunUpsertCommand toCommand(
      Long prNumber, String headSha, GithubPullRequestCheckState state, Instant now) {
    return switch (state) {
      case ON_TRACK ->
          new GithubCheckRunUpsertCommand(
              CHECK_NAME,
              headSha,
              "pr-" + prNumber,
              "completed",
              "success",
              "On track",
              "Review SLA is running and no threshold has been reached yet.",
              "The PR is being tracked for its first review.",
              now,
              now);
      case AT_RISK ->
          new GithubCheckRunUpsertCommand(
              CHECK_NAME,
              headSha,
              "pr-" + prNumber,
              "completed",
              "neutral",
              "Review at risk",
              "This PR has crossed the 12h reminder threshold without a review.",
              "A reminder notification has been sent.",
              now,
              now);
      case BREACHED ->
          new GithubCheckRunUpsertCommand(
              CHECK_NAME,
              headSha,
              "pr-" + prNumber,
              "completed",
              "action_required",
              "Review breached",
              "This PR has crossed the 24h SLA threshold without a review.",
              "Escalation or fallback notification has been sent.",
              now,
              now);
      case REVIEWED ->
          new GithubCheckRunUpsertCommand(
              CHECK_NAME,
              headSha,
              "pr-" + prNumber,
              "completed",
              "success",
              "Reviewed",
              "A first review has been recorded.",
              "SLA tracking is complete for this revision.",
              now,
              now);
      case APP_NOT_CONFIGURED ->
          new GithubCheckRunUpsertCommand(
              CHECK_NAME,
              headSha,
              "pr-" + prNumber,
              "completed",
              "action_required",
              "App setup required",
              "The GitHub App is installed, but Discord is not configured for this repository yet.",
              "Open the installation settings page and save a Discord webhook to start tracking.",
              now,
              now);
    };
  }
}

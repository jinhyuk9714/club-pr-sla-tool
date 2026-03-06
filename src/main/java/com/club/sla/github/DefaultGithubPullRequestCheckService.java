package com.club.sla.github;

import com.club.sla.delivery.OutboundDeliveryJobService;
import org.springframework.stereotype.Service;

@Service
public class DefaultGithubPullRequestCheckService implements GithubPullRequestCheckService {

  private final OutboundDeliveryJobService outboundDeliveryJobService;

  public DefaultGithubPullRequestCheckService(
      OutboundDeliveryJobService outboundDeliveryJobService) {
    this.outboundDeliveryJobService = outboundDeliveryJobService;
  }

  @Override
  public void syncStatus(
      Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state) {
    if (headSha == null || headSha.isBlank()) {
      return;
    }
    outboundDeliveryJobService.enqueueGithubCheckSync(repositoryId, prNumber, headSha, state);
  }
}

package com.club.sla.github;

public interface GithubCheckDeliveryService {

  void deliver(Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state);
}

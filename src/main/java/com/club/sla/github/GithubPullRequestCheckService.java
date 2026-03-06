package com.club.sla.github;

public interface GithubPullRequestCheckService {

  void syncStatus(
      Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state);
}

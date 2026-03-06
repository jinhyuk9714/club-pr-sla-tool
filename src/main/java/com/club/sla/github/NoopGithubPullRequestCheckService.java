package com.club.sla.github;

public class NoopGithubPullRequestCheckService implements GithubPullRequestCheckService {

  @Override
  public void syncStatus(
      Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state) {
    // The GitHub Check transport is wired in a later integration step.
  }
}

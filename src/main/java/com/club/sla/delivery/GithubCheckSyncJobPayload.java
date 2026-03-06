package com.club.sla.delivery;

import com.club.sla.github.GithubPullRequestCheckState;

public record GithubCheckSyncJobPayload(
    Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState targetState) {}

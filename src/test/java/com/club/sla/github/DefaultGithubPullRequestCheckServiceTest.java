package com.club.sla.github;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.club.sla.delivery.OutboundDeliveryJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultGithubPullRequestCheckServiceTest {

  @Mock private OutboundDeliveryJobService outboundDeliveryJobService;

  private DefaultGithubPullRequestCheckService defaultGithubPullRequestCheckService;

  @BeforeEach
  void setUp() {
    defaultGithubPullRequestCheckService =
        new DefaultGithubPullRequestCheckService(outboundDeliveryJobService);
  }

  @Test
  void enqueuesGithubCheckSyncForTrackedPullRequestState() {
    defaultGithubPullRequestCheckService.syncStatus(
        9001L, 77L, "head-123", GithubPullRequestCheckState.AT_RISK);

    verify(outboundDeliveryJobService)
        .enqueueGithubCheckSync(9001L, 77L, "head-123", GithubPullRequestCheckState.AT_RISK);
  }

  @Test
  void skipsSyncWhenHeadShaIsBlank() {
    defaultGithubPullRequestCheckService.syncStatus(
        9001L, 77L, " ", GithubPullRequestCheckState.ON_TRACK);

    verify(outboundDeliveryJobService, never())
        .enqueueGithubCheckSync(9001L, 77L, " ", GithubPullRequestCheckState.ON_TRACK);
  }
}

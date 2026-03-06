package com.club.sla.delivery;

import com.club.sla.github.GithubPullRequestCheckState;
import com.club.sla.notify.NotificationMessage;

public interface OutboundDeliveryJobService {

  void enqueueDiscordNotification(NotificationMessage message);

  void enqueueGithubCheckSync(
      Long repositoryId, Long prNumber, String headSha, GithubPullRequestCheckState state);

  void enqueueReplay(OutboundDeliveryJobType jobType, String payloadJson);
}

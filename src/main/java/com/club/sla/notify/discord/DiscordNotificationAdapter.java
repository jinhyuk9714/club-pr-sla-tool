package com.club.sla.notify.discord;

import com.club.sla.installation.InstallationTrackingService;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.NotificationPort;
import org.springframework.stereotype.Component;

@Component
public class DiscordNotificationAdapter implements NotificationPort {

  private final DiscordWebhookClient discordWebhookClient;
  private final InstallationTrackingService installationTrackingService;

  public DiscordNotificationAdapter(
      DiscordWebhookClient discordWebhookClient,
      InstallationTrackingService installationTrackingService) {
    this.discordWebhookClient = discordWebhookClient;
    this.installationTrackingService = installationTrackingService;
  }

  @Override
  public void send(NotificationMessage message) {
    String webhookUrl =
        installationTrackingService
            .findConfiguredDiscordWebhook(message.repoId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No configured Discord webhook for repository " + message.repoId()));
    DiscordWebhookClient.DiscordWebhookPayload payload =
        new DiscordWebhookClient.DiscordWebhookPayload(buildNotificationContent(message));

    discordWebhookClient.post(webhookUrl, payload);
  }

  private String buildNotificationContent(NotificationMessage message) {
    return "[SLA] repo=%d pr=%d stage=%s"
        .formatted(message.repoId(), message.prNumber(), message.stage().name());
  }
}

package com.club.sla.notify.discord;

import com.club.sla.notify.DeadLetterEvent;
import com.club.sla.notify.DeadLetterRepository;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.NotificationPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiscordNotificationAdapter implements NotificationPort {

  private static final int MAX_ATTEMPTS = 3;
  private static final String DEAD_LETTER_REASON = "DISCORD_SEND_FAILED";

  private final DiscordWebhookClient discordWebhookClient;
  private final DeadLetterRepository deadLetterRepository;
  private final Clock clock = Clock.systemUTC();
  private final String webhookUrl;

  public DiscordNotificationAdapter(
      DiscordWebhookClient discordWebhookClient,
      DeadLetterRepository deadLetterRepository,
      @Value("${discord.webhook.url:}") String webhookUrl) {
    this.discordWebhookClient = discordWebhookClient;
    this.deadLetterRepository = deadLetterRepository;
    this.webhookUrl = webhookUrl;
  }

  @Override
  public void send(NotificationMessage message) {
    RuntimeException lastException = null;
    DiscordWebhookClient.DiscordWebhookPayload payload =
        new DiscordWebhookClient.DiscordWebhookPayload(buildNotificationContent(message));

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        if (webhookUrl == null || webhookUrl.isBlank()) {
          throw new IllegalStateException("discord.webhook.url is not configured");
        }
        discordWebhookClient.post(webhookUrl, payload);
        return;
      } catch (RuntimeException ex) {
        lastException = ex;
      }
    }

    deadLetterRepository.save(
        new DeadLetterEvent(
            DEAD_LETTER_REASON,
            payload.content(),
            Instant.now(clock),
            message.repoId(),
            message.prNumber(),
            message.stage()));

    throw lastException == null
        ? new IllegalStateException("discord notification failed without exception details")
        : lastException;
  }

  private String buildNotificationContent(NotificationMessage message) {
    return "[SLA] repo=%d pr=%d stage=%s"
        .formatted(message.repoId(), message.prNumber(), message.stage().name());
  }
}

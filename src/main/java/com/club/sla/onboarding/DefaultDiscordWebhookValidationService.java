package com.club.sla.onboarding;

import com.club.sla.notify.discord.DiscordWebhookClient;
import org.springframework.stereotype.Service;

@Service
public class DefaultDiscordWebhookValidationService implements DiscordWebhookValidationService {

  private final DiscordWebhookClient discordWebhookClient;

  public DefaultDiscordWebhookValidationService(DiscordWebhookClient discordWebhookClient) {
    this.discordWebhookClient = discordWebhookClient;
  }

  @Override
  public void validate(String discordWebhookUrl) {
    discordWebhookClient.post(
        discordWebhookUrl,
        new DiscordWebhookClient.DiscordWebhookPayload(
            "[Club PR SLA Tool] Discord webhook connected."));
  }
}

package com.club.sla.notify.discord;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DiscordWebhookClient {

  private final RestClient restClient;

  public DiscordWebhookClient(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  public void post(String webhookUrl, DiscordWebhookPayload payload) {
    restClient
        .post()
        .uri(webhookUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  public record DiscordWebhookPayload(String content) {}
}

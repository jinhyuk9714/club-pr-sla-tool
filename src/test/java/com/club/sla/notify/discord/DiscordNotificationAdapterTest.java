package com.club.sla.notify.discord;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.installation.InstallationTrackingService;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.sla.SlaAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationAdapterTest {

  @Mock private DiscordWebhookClient discordWebhookClient;

  @Mock private InstallationTrackingService installationTrackingService;

  @InjectMocks private DiscordNotificationAdapter discordNotificationAdapter;

  @Test
  void usesConfiguredInstallationWebhookWhenFirstAttemptSucceeds() {
    NotificationMessage message = new NotificationMessage(10L, 100L, SlaAction.REMIND_12H);
    when(installationTrackingService.findConfiguredDiscordWebhook(10L))
        .thenReturn(java.util.Optional.of("https://discord.example/webhook"));

    doNothing()
        .when(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));

    discordNotificationAdapter.send(message);

    verify(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));
  }

  @Test
  void throwsImmediatelyWhenWebhookPostFails() {
    NotificationMessage message = new NotificationMessage(10L, 101L, SlaAction.ESCALATE_24H);
    when(installationTrackingService.findConfiguredDiscordWebhook(10L))
        .thenReturn(java.util.Optional.of("https://discord.example/webhook"));

    doThrow(new RuntimeException("boom"))
        .when(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));

    assertThatThrownBy(() -> discordNotificationAdapter.send(message))
        .isInstanceOf(RuntimeException.class);

    verify(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));
  }

  @Test
  void throwsWhenRepositoryHasNoConfiguredWebhook() {
    NotificationMessage message = new NotificationMessage(10L, 103L, SlaAction.REMIND_12H);
    when(installationTrackingService.findConfiguredDiscordWebhook(10L))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> discordNotificationAdapter.send(message))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No configured Discord webhook");

    verify(discordWebhookClient, never())
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));
  }
}

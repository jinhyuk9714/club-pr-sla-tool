package com.club.sla.notify.discord;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.club.sla.notify.DeadLetterEvent;
import com.club.sla.notify.DeadLetterRepository;
import com.club.sla.notify.NotificationMessage;
import com.club.sla.sla.SlaAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationAdapterTest {

  @Mock private DiscordWebhookClient discordWebhookClient;

  @Mock private DeadLetterRepository deadLetterRepository;

  @InjectMocks private DiscordNotificationAdapter discordNotificationAdapter;

  @Test
  void doesNotStoreDeadLetterWhenFirstAttemptSucceeds() {
    ReflectionTestUtils.setField(
        discordNotificationAdapter, "webhookUrl", "https://discord.example/webhook");
    NotificationMessage message = new NotificationMessage(10L, 100L, SlaAction.REMIND_12H);

    doNothing()
        .when(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));

    discordNotificationAdapter.send(message);

    verify(discordWebhookClient, times(1))
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));
    verify(deadLetterRepository, never()).save(any(DeadLetterEvent.class));
  }

  @Test
  void storesDeadLetterAndThrowsWhenThreeAttemptsFail() {
    ReflectionTestUtils.setField(
        discordNotificationAdapter, "webhookUrl", "https://discord.example/webhook");
    NotificationMessage message = new NotificationMessage(10L, 101L, SlaAction.ESCALATE_24H);

    doThrow(new RuntimeException("boom"))
        .when(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));

    assertThatThrownBy(() -> discordNotificationAdapter.send(message))
        .isInstanceOf(RuntimeException.class);

    verify(discordWebhookClient, times(3))
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));
    verify(deadLetterRepository, times(1)).save(any(DeadLetterEvent.class));
  }

  @Test
  void doesNotStoreDeadLetterWhenThirdAttemptSucceeds() {
    ReflectionTestUtils.setField(
        discordNotificationAdapter, "webhookUrl", "https://discord.example/webhook");
    NotificationMessage message = new NotificationMessage(10L, 102L, SlaAction.FALLBACK_36H);

    doThrow(new RuntimeException("boom-1"))
        .doThrow(new RuntimeException("boom-2"))
        .doNothing()
        .when(discordWebhookClient)
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));

    discordNotificationAdapter.send(message);

    verify(discordWebhookClient, times(3))
        .post(anyString(), any(DiscordWebhookClient.DiscordWebhookPayload.class));
    verify(deadLetterRepository, never()).save(any(DeadLetterEvent.class));
  }
}

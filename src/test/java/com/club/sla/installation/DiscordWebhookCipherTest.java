package com.club.sla.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DiscordWebhookCipherTest {

  @Test
  void encryptsAndDecryptsWebhookUrl() {
    DiscordWebhookCipher discordWebhookCipher =
        new DiscordWebhookCipher("local-encryption-secret-123");

    String encrypted = discordWebhookCipher.encrypt("https://discord.example/webhook");

    assertThat(encrypted).isNotEqualTo("https://discord.example/webhook");
    assertThat(discordWebhookCipher.decrypt(encrypted))
        .isEqualTo("https://discord.example/webhook");
  }

  @Test
  void rejectsCipherTextProducedWithDifferentSecret() {
    DiscordWebhookCipher sourceCipher = new DiscordWebhookCipher("local-encryption-secret-123");
    DiscordWebhookCipher otherCipher = new DiscordWebhookCipher("other-encryption-secret-123");

    String encrypted = sourceCipher.encrypt("https://discord.example/webhook");

    assertThatThrownBy(() -> otherCipher.decrypt(encrypted))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to decrypt");
  }
}

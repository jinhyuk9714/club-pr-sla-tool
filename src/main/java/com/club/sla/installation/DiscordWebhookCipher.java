package com.club.sla.installation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiscordWebhookCipher {

  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final int NONCE_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKeySpec secretKeySpec;
  private final SecureRandom secureRandom = new SecureRandom();

  public DiscordWebhookCipher(
      @Value("${app.security.encryption-secret:local-dev-encryption-secret}") String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("app.security.encryption-secret must not be blank");
    }
    this.secretKeySpec = new SecretKeySpec(sha256(secret), "AES");
  }

  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      throw new IllegalArgumentException("plaintext must not be blank");
    }
    try {
      byte[] nonce = new byte[NONCE_LENGTH];
      secureRandom.nextBytes(nonce);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      ByteBuffer buffer = ByteBuffer.allocate(nonce.length + encrypted.length);
      buffer.put(nonce);
      buffer.put(encrypted);
      return Base64.getEncoder().encodeToString(buffer.array());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encrypt discord webhook", ex);
    }
  }

  public String decrypt(String cipherText) {
    if (cipherText == null || cipherText.isBlank()) {
      throw new IllegalArgumentException("cipherText must not be blank");
    }
    try {
      byte[] payload = Base64.getDecoder().decode(cipherText);
      ByteBuffer buffer = ByteBuffer.wrap(payload);
      byte[] nonce = new byte[NONCE_LENGTH];
      buffer.get(nonce);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      byte[] decrypted = cipher.doFinal(encrypted);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decrypt discord webhook", ex);
    }
  }

  private byte[] sha256(String input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize encryption key", ex);
    }
  }
}

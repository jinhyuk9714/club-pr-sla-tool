package com.club.sla.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GithubSignatureVerifier {

  private static final String SIGNATURE_PREFIX = "sha256=";

  private final String webhookSecret;

  public GithubSignatureVerifier(@Value("${github.webhook.secret:}") String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public boolean isValid(String payload, String signatureHeader) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      return false;
    }
    if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
      return false;
    }

    String expected = SIGNATURE_PREFIX + hmacSha256Hex(payload, webhookSecret);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        signatureHeader.getBytes(StandardCharsets.UTF_8));
  }

  private String hmacSha256Hex(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(digest.length * 2);
      for (byte value : digest) {
        builder.append(String.format("%02x", value));
      }
      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to verify GitHub webhook signature", ex);
    }
  }
}

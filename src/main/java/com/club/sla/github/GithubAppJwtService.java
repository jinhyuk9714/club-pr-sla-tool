package com.club.sla.github;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GithubAppJwtService {

  private final String appId;
  private final String privateKeyPem;

  public GithubAppJwtService(
      @Value("${github.app.app-id:}") String appId,
      @Value("${github.app.private-key:}") String privateKeyPem) {
    this.appId = appId;
    this.privateKeyPem = privateKeyPem;
  }

  public String createJwt() {
    if (appId == null || appId.isBlank()) {
      throw new IllegalStateException("github.app.app-id must not be blank");
    }
    long issuedAt = Instant.now().getEpochSecond();
    long expiresAt = issuedAt + 540;
    String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
    String payload =
        base64Url(
            """
            {"iat":%d,"exp":%d,"iss":"%s"}
            """
                .formatted(issuedAt, expiresAt, appId)
                .replace("\n", "")
                .trim());
    String signingInput = header + "." + payload;
    return signingInput + "." + sign(signingInput, parsePrivateKey(privateKeyPem));
  }

  private PrivateKey parsePrivateKey(String privateKeyPem) {
    try {
      if (privateKeyPem == null || privateKeyPem.isBlank()) {
        throw new IllegalStateException("github.app.private-key must not be blank");
      }
      String normalized =
          privateKeyPem
              .replace("\\n", "\n")
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(normalized);
      return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse GitHub App private key", ex);
    }
  }

  private String sign(String signingInput, PrivateKey privateKey) {
    try {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(privateKey);
      signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to sign GitHub App JWT", ex);
    }
  }

  private String base64Url(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}

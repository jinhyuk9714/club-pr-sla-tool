package com.club.sla.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class GithubAppJwtServiceTest {

  @Test
  void createsSignedJwtForGithubApp() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
    GithubAppJwtService githubAppJwtService = new GithubAppJwtService("12345", pem);

    String jwt = githubAppJwtService.createJwt();

    assertThat(jwt.split("\\.")).hasSize(3);
    String header =
        new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]), StandardCharsets.UTF_8);
    assertThat(header).contains("\"alg\":\"RS256\"");
  }

  @Test
  void createsSignedJwtWhenPrivateKeyUsesEscapedNewlines() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
    String envStylePem = pem.replace("\n", "\\n");
    GithubAppJwtService githubAppJwtService = new GithubAppJwtService("12345", envStylePem);

    String jwt = githubAppJwtService.createJwt();

    assertThat(jwt.split("\\.")).hasSize(3);
    String header =
        new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]), StandardCharsets.UTF_8);
    assertThat(header).contains("\"alg\":\"RS256\"");
  }
}

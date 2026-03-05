package com.club.sla.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class GithubWebhookController {

  private final GithubSignatureVerifier githubSignatureVerifier;
  private final WebhookIngestionPort webhookIngestionPort;

  public GithubWebhookController(
      GithubSignatureVerifier githubSignatureVerifier, WebhookIngestionPort webhookIngestionPort) {
    this.githubSignatureVerifier = githubSignatureVerifier;
    this.webhookIngestionPort = webhookIngestionPort;
  }

  @PostMapping("/github")
  public ResponseEntity<Void> receive(
      @RequestHeader("X-Hub-Signature-256") String signature,
      @RequestHeader("X-GitHub-Delivery") String deliveryId,
      @RequestHeader("X-GitHub-Event") String eventType,
      @RequestBody String payload) {
    if (!githubSignatureVerifier.isValid(payload, signature)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    webhookIngestionPort.ingest(deliveryId, eventType, payload);
    return ResponseEntity.accepted().build();
  }
}

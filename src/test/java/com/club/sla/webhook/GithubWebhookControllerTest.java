package com.club.sla.webhook;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GithubWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class GithubWebhookControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GithubSignatureVerifier githubSignatureVerifier;

  @MockBean private WebhookIngestionPort webhookIngestionPort;

  @Test
  void returnsUnauthorizedWhenSignatureIsInvalid() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(false);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=invalid")
                .header("X-GitHub-Delivery", "delivery-1")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());

    verify(webhookIngestionPort, never()).ingest(anyString(), anyString(), anyString());
  }

  @Test
  void returnsAcceptedWhenSignatureIsValid() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-2")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"opened\"}"))
        .andExpect(status().isAccepted());

    verify(webhookIngestionPort).ingest("delivery-2", "pull_request", "{\"action\":\"opened\"}");
  }

  @Test
  void returnsBadRequestWhenDeliveryHeaderIsMissing() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());

    verify(webhookIngestionPort, never()).ingest(anyString(), anyString(), anyString());
  }
}

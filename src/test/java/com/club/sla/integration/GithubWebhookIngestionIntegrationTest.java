package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import com.club.sla.webhook.DeliveryEventRepository;
import com.club.sla.webhook.GithubSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class GithubWebhookIngestionIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("club_sla")
          .withUsername("club_sla")
          .withPassword("club_sla");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private PullRequestStateRepository pullRequestStateRepository;

  @Autowired private DeliveryEventRepository deliveryEventRepository;

  @MockBean private GithubSignatureVerifier githubSignatureVerifier;

  @BeforeEach
  void clearDatabase() {
    pullRequestStateRepository.deleteAll();
    deliveryEventRepository.deleteAll();
  }

  @Test
  void ingestsReadyForReviewEvent() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-101")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readyForReviewPayload(9001L, 17L, "alice")))
        .andExpect(status().isAccepted());

    PullRequestState state =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(9001L, 17L).orElseThrow();
    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.READY);
    assertThat(state.getReadyAt()).isNotNull();
    assertThat(deliveryEventRepository.count()).isEqualTo(1);
  }

  @Test
  void ignoresDuplicateDeliveryId() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-202")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readyForReviewPayload(9002L, 18L, "alice")))
        .andExpect(status().isAccepted());

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-202")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertedToDraftPayload(9002L, 18L, "alice")))
        .andExpect(status().isAccepted());

    PullRequestState state =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(9002L, 18L).orElseThrow();
    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.READY);
    assertThat(deliveryEventRepository.count()).isEqualTo(1);
  }

  @Test
  void storesFirstReviewTimestampWhenReviewSubmittedEventArrives() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-301")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readyForReviewPayload(9003L, 19L, "alice")))
        .andExpect(status().isAccepted());

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-302")
                .header("X-GitHub-Event", "pull_request_review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewSubmittedPayload(9003L, 19L, "mentor")))
        .andExpect(status().isAccepted());

    PullRequestState state =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(9003L, 19L).orElseThrow();
    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.REVIEWED);
    assertThat(state.getFirstReviewAt()).isNotNull();
    assertThat(deliveryEventRepository.count()).isEqualTo(2);
  }

  private String readyForReviewPayload(Long repositoryId, Long number, String authorLogin) {
    return """
        {
          "action": "ready_for_review",
          "repository": {"id": %d},
          "pull_request": {
            "number": %d,
            "user": {"login": "%s"}
          }
        }
        """
        .formatted(repositoryId, number, authorLogin);
  }

  private String convertedToDraftPayload(Long repositoryId, Long number, String authorLogin) {
    return """
        {
          "action": "converted_to_draft",
          "repository": {"id": %d},
          "pull_request": {
            "number": %d,
            "user": {"login": "%s"}
          }
        }
        """
        .formatted(repositoryId, number, authorLogin);
  }

  private String reviewSubmittedPayload(Long repositoryId, Long number, String reviewerLogin) {
    return """
        {
          "action": "submitted",
          "repository": {"id": %d},
          "pull_request": {
            "number": %d
          },
          "review": {
            "user": {"login": "%s"}
          }
        }
        """
        .formatted(repositoryId, number, reviewerLogin);
  }
}

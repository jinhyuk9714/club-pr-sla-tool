package com.club.sla.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.club.sla.github.GithubPullRequestCheckService;
import com.club.sla.github.GithubPullRequestCheckState;
import com.club.sla.installation.GithubInstallationAccountType;
import com.club.sla.installation.GithubInstallationRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntryRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntryUpsertCommand;
import com.club.sla.installation.GithubInstallationUpsertCommand;
import com.club.sla.installation.InstallationRegistryService;
import com.club.sla.installation.InstallationSettingsRepository;
import com.club.sla.pr.PullRequestState;
import com.club.sla.pr.PullRequestStateRepository;
import com.club.sla.pr.PullRequestStatus;
import com.club.sla.webhook.DeliveryEventRepository;
import com.club.sla.webhook.GithubSignatureVerifier;
import java.time.Instant;
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
    registry.add("app.security.encryption-secret", () -> "integration-test-encryption-secret");
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private InstallationRegistryService installationRegistryService;
  @Autowired private GithubInstallationRepository githubInstallationRepository;

  @Autowired
  private GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository;

  @Autowired private InstallationSettingsRepository installationSettingsRepository;
  @Autowired private PullRequestStateRepository pullRequestStateRepository;

  @Autowired private DeliveryEventRepository deliveryEventRepository;

  @MockBean private GithubSignatureVerifier githubSignatureVerifier;
  @MockBean private GithubPullRequestCheckService githubPullRequestCheckService;

  @BeforeEach
  void clearDatabase() {
    pullRequestStateRepository.deleteAll();
    deliveryEventRepository.deleteAll();
    installationSettingsRepository.deleteAll();
    githubInstallationRepositoryEntryRepository.deleteAll();
    githubInstallationRepository.deleteAll();
  }

  @Test
  void createsInstallationAndRepositoriesWhenInstallationCreatedEventArrives() throws Exception {
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-install-101")
                .header("X-GitHub-Event", "installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(installationCreatedPayload(7001L, 9001L, "club-org", "club-pr-tool")))
        .andExpect(status().isAccepted());

    assertThat(githubInstallationRepository.findById(7001L)).isPresent();
    assertThat(githubInstallationRepositoryEntryRepository.findByRepositoryId(9001L)).isPresent();
    assertThat(deliveryEventRepository.count()).isEqualTo(1);
  }

  @Test
  void doesNotStartTrackingWhenInstallationSettingsAreMissing() throws Exception {
    seedInstallation(7002L, 9002L);
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-202")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readyForReviewPayload(7002L, 9002L, 18L, "alice", "head-sha-18")))
        .andExpect(status().isAccepted());

    assertThat(pullRequestStateRepository.findByRepositoryIdAndPrNumber(9002L, 18L)).isEmpty();
    verify(githubPullRequestCheckService)
        .syncStatus(9002L, 18L, "head-sha-18", GithubPullRequestCheckState.APP_NOT_CONFIGURED);
  }

  @Test
  void tracksReadyForReviewWhenInstallationIsConfigured() throws Exception {
    seedConfiguredInstallation(7003L, 9003L);
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-301")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readyForReviewPayload(7003L, 9003L, 19L, "alice", "head-sha-19")))
        .andExpect(status().isAccepted());

    PullRequestState state =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(9003L, 19L).orElseThrow();
    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.READY);
    assertThat(state.getReadyAt()).isNotNull();
    assertThat(state.getHeadSha()).isEqualTo("head-sha-19");
    verify(githubPullRequestCheckService)
        .syncStatus(9003L, 19L, "head-sha-19", GithubPullRequestCheckState.ON_TRACK);
  }

  @Test
  void storesFirstReviewTimestampWhenReviewSubmittedEventArrives() throws Exception {
    seedConfiguredInstallation(7004L, 9004L);
    given(githubSignatureVerifier.isValid(anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-401")
                .header("X-GitHub-Event", "pull_request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readyForReviewPayload(7004L, 9004L, 20L, "alice", "head-sha-20")))
        .andExpect(status().isAccepted());

    mockMvc
        .perform(
            post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-402")
                .header("X-GitHub-Event", "pull_request_review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewSubmittedPayload(7004L, 9004L, 20L, "mentor", "head-sha-20")))
        .andExpect(status().isAccepted());

    PullRequestState state =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(9004L, 20L).orElseThrow();
    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.REVIEWED);
    assertThat(state.getFirstReviewAt()).isNotNull();
    assertThat(deliveryEventRepository.count()).isEqualTo(2);
    verify(githubPullRequestCheckService)
        .syncStatus(9004L, 20L, "head-sha-20", GithubPullRequestCheckState.REVIEWED);
  }

  private void seedInstallation(Long installationId, Long repositoryId) {
    Instant installedAt = Instant.parse("2026-03-06T00:00:00Z");
    installationRegistryService.upsertInstallation(
        new GithubInstallationUpsertCommand(
            installationId,
            installationId + 1000,
            "club-org",
            GithubInstallationAccountType.ORGANIZATION,
            installedAt));
    installationRegistryService.syncRepositories(
        installationId,
        java.util.List.of(
            new GithubInstallationRepositoryEntryUpsertCommand(
                repositoryId, "club-pr-tool", "club-org/club-pr-tool")));
  }

  private void seedConfiguredInstallation(Long installationId, Long repositoryId) {
    seedInstallation(installationId, repositoryId);
    installationRegistryService.saveSettings(
        installationId,
        new com.club.sla.installation.InstallationSettingsUpdateCommand(
            "https://discord.example/webhook", true));
  }

  private String installationCreatedPayload(
      Long installationId, Long repositoryId, String accountLogin, String repositoryName) {
    return """
        {
          "action": "created",
          "installation": {
            "id": %d,
            "account": {"id": 991, "login": "%s", "type": "Organization"},
            "created_at": "2026-03-06T00:00:00Z"
          },
          "repositories": [
            {"id": %d, "name": "%s", "full_name": "%s/%s"}
          ]
        }
        """
        .formatted(
            installationId,
            accountLogin,
            repositoryId,
            repositoryName,
            accountLogin,
            repositoryName);
  }

  private String readyForReviewPayload(
      Long installationId, Long repositoryId, Long number, String authorLogin, String headSha) {
    return """
        {
          "action": "ready_for_review",
          "installation": {"id": %d},
          "repository": {"id": %d},
          "pull_request": {
            "number": %d,
            "user": {"login": "%s"},
            "head": {"sha": "%s"}
          }
        }
        """
        .formatted(installationId, repositoryId, number, authorLogin, headSha);
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

  private String reviewSubmittedPayload(
      Long installationId, Long repositoryId, Long number, String reviewerLogin, String headSha) {
    return """
        {
          "action": "submitted",
          "installation": {"id": %d},
          "repository": {"id": %d},
          "pull_request": {
            "number": %d,
            "head": {"sha": "%s"}
          },
          "review": {
            "user": {"login": "%s"}
          }
        }
        """
        .formatted(installationId, repositoryId, number, headSha, reviewerLogin);
  }
}

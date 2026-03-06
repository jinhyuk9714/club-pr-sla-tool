package com.club.sla.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.club.sla.installation.GithubInstallationAccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class RestGithubAppClientTest {

  @Mock private GithubAppJwtService githubAppJwtService;

  @Test
  void exchangesOauthCodeForAuthenticatedUser() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer mockRestServiceServer =
        MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(true).build();
    RestGithubAppClient restGithubAppClient =
        new RestGithubAppClient(
            restClientBuilder,
            githubAppJwtService,
            "client-id",
            "client-secret",
            "https://github.com",
            "https://api.github.test");

    mockRestServiceServer
        .expect(requestTo("https://github.com/login/oauth/access_token"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"access_token\":\"user-token\",\"token_type\":\"bearer\"}",
                MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(requestTo("https://api.github.test/user"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer user-token"))
        .andRespond(withSuccess("{\"id\":101,\"login\":\"alice\"}", MediaType.APPLICATION_JSON));

    GithubAuthenticatedUser authenticatedUser =
        restGithubAppClient.exchangeCodeForUser(
            "oauth-code", "https://club-pr.example/auth/github/callback");

    assertThat(authenticatedUser)
        .isEqualTo(new GithubAuthenticatedUser(101L, "alice", "user-token"));
  }

  @Test
  void fetchesInstallationAndRepositories() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer mockRestServiceServer =
        MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(true).build();
    RestGithubAppClient restGithubAppClient =
        new RestGithubAppClient(
            restClientBuilder,
            githubAppJwtService,
            "client-id",
            "client-secret",
            "https://github.com",
            "https://api.github.test");
    given(githubAppJwtService.createJwt()).willReturn("app-jwt");

    mockRestServiceServer
        .expect(requestTo("https://api.github.test/app/installations/7001"))
        .andExpect(header("Authorization", "Bearer app-jwt"))
        .andRespond(
            withSuccess(
                """
                {
                  "id": 7001,
                  "account": {"id": 991, "login": "club-org", "type": "Organization"},
                  "created_at": "2026-03-06T00:00:00Z"
                }
                """,
                MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(requestTo("https://api.github.test/app/installations/7001/access_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer app-jwt"))
        .andRespond(withSuccess("{\"token\":\"installation-token\"}", MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(requestTo("https://api.github.test/installation/repositories"))
        .andExpect(header("Authorization", "Bearer installation-token"))
        .andRespond(
            withSuccess(
                """
                {
                  "repositories": [
                    {"id": 9001, "name": "club-pr-tool", "full_name": "club-org/club-pr-tool"},
                    {"id": 9002, "name": "club-web", "full_name": "club-org/club-web"}
                  ]
                }
                """,
                MediaType.APPLICATION_JSON));

    GithubInstallationMetadata installationMetadata = restGithubAppClient.fetchInstallation(7001L);
    assertThat(installationMetadata)
        .isEqualTo(
            new GithubInstallationMetadata(
                7001L,
                991L,
                "club-org",
                GithubInstallationAccountType.ORGANIZATION,
                java.time.Instant.parse("2026-03-06T00:00:00Z")));

    assertThat(restGithubAppClient.listInstallationRepositories(7001L))
        .containsExactly(
            new GithubRepositoryMetadata(9001L, "club-pr-tool", "club-org/club-pr-tool"),
            new GithubRepositoryMetadata(9002L, "club-web", "club-org/club-web"));
  }

  @Test
  void createsCheckRunWhenNoExistingRunMatchesHeadSha() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer mockRestServiceServer =
        MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(true).build();
    RestGithubAppClient restGithubAppClient =
        new RestGithubAppClient(
            restClientBuilder,
            githubAppJwtService,
            "client-id",
            "client-secret",
            "https://github.com",
            "https://api.github.test");
    given(githubAppJwtService.createJwt()).willReturn("app-jwt");

    mockRestServiceServer
        .expect(requestTo("https://api.github.test/app/installations/7001/access_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"token\":\"installation-token\"}", MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.test/repos/club-org/club-pr-tool/commits/head-123/check-runs?check_name=Club%20PR%20SLA&filter=latest&per_page=1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer installation-token"))
        .andRespond(
            withSuccess("{\"total_count\":0,\"check_runs\":[]}", MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(requestTo("https://api.github.test/repos/club-org/club-pr-tool/check-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer installation-token"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "name": "Club PR SLA",
                      "head_sha": "head-123",
                      "external_id": "pr-77",
                      "status": "completed",
                      "conclusion": "neutral"
                    }
                    """,
                    false))
        .andRespond(withSuccess("{\"id\":2001}", MediaType.APPLICATION_JSON));

    restGithubAppClient.upsertCheckRun(
        7001L,
        "club-org/club-pr-tool",
        new GithubCheckRunUpsertCommand(
            "Club PR SLA",
            "head-123",
            "pr-77",
            "completed",
            "neutral",
            "Review at risk",
            "This PR has crossed the 12h reminder threshold without a review.",
            "Discord reminder sent.",
            java.time.Instant.parse("2026-03-06T00:00:00Z"),
            java.time.Instant.parse("2026-03-06T00:00:00Z")));
  }

  @Test
  void updatesExistingCheckRunWhenLatestRunExists() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer mockRestServiceServer =
        MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(true).build();
    RestGithubAppClient restGithubAppClient =
        new RestGithubAppClient(
            restClientBuilder,
            githubAppJwtService,
            "client-id",
            "client-secret",
            "https://github.com",
            "https://api.github.test");
    given(githubAppJwtService.createJwt()).willReturn("app-jwt");

    mockRestServiceServer
        .expect(requestTo("https://api.github.test/app/installations/7001/access_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"token\":\"installation-token\"}", MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.test/repos/club-org/club-pr-tool/commits/head-123/check-runs?check_name=Club%20PR%20SLA&filter=latest&per_page=1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer installation-token"))
        .andRespond(
            withSuccess(
                "{\"total_count\":1,\"check_runs\":[{\"id\":2001}]}", MediaType.APPLICATION_JSON));
    mockRestServiceServer
        .expect(requestTo("https://api.github.test/repos/club-org/club-pr-tool/check-runs/2001"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(header("Authorization", "Bearer installation-token"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "name": "Club PR SLA",
                      "status": "completed",
                      "conclusion": "success"
                    }
                    """,
                    false))
        .andRespond(withSuccess("{\"id\":2001}", MediaType.APPLICATION_JSON));

    restGithubAppClient.upsertCheckRun(
        7001L,
        "club-org/club-pr-tool",
        new GithubCheckRunUpsertCommand(
            "Club PR SLA",
            "head-123",
            "pr-77",
            "completed",
            "success",
            "Reviewed",
            "A first review has been recorded.",
            "SLA tracking is complete for this revision.",
            java.time.Instant.parse("2026-03-06T00:00:00Z"),
            java.time.Instant.parse("2026-03-06T00:00:00Z")));
  }
}

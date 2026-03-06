package com.club.sla.github;

import com.club.sla.installation.GithubInstallationAccountType;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class RestGithubAppClient implements GithubAppClient {

  private static final String API_VERSION = "2022-11-28";
  private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";

  private final RestClient authRestClient;
  private final RestClient apiRestClient;
  private final GithubAppJwtService githubAppJwtService;
  private final String clientId;
  private final String clientSecret;

  public RestGithubAppClient(
      RestClient.Builder restClientBuilder,
      GithubAppJwtService githubAppJwtService,
      @Value("${github.app.client-id:}") String clientId,
      @Value("${github.app.client-secret:}") String clientSecret,
      @Value("${github.app.auth-base-url:https://github.com}") String authBaseUrl,
      @Value("${github.app.api-base-url:https://api.github.com}") String apiBaseUrl) {
    this.authRestClient = restClientBuilder.baseUrl(authBaseUrl).build();
    this.apiRestClient = restClientBuilder.baseUrl(apiBaseUrl).build();
    this.githubAppJwtService = githubAppJwtService;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public GithubAuthenticatedUser exchangeCodeForUser(String code, String redirectUri) {
    GithubAccessTokenResponse githubAccessTokenResponse =
        authRestClient
            .post()
            .uri("/login/oauth/access_token")
            .header(HttpHeaders.ACCEPT, "application/json")
            .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                "client_id=%s&client_secret=%s&code=%s&redirect_uri=%s"
                    .formatted(clientId, clientSecret, code, redirectUri))
            .retrieve()
            .body(GithubAccessTokenResponse.class);
    if (githubAccessTokenResponse == null || githubAccessTokenResponse.accessToken() == null) {
      throw new IllegalStateException("GitHub OAuth token exchange returned no access token");
    }

    GithubUserResponse githubUserResponse =
        apiRestClient
            .get()
            .uri("/user")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubAccessTokenResponse.accessToken())
            .header("X-GitHub-Api-Version", API_VERSION)
            .retrieve()
            .body(GithubUserResponse.class);
    if (githubUserResponse == null) {
      throw new IllegalStateException("GitHub user lookup returned no user");
    }
    return new GithubAuthenticatedUser(
        githubUserResponse.id(),
        githubUserResponse.login(),
        githubAccessTokenResponse.accessToken());
  }

  @Override
  public boolean userCanAccessInstallation(String userAccessToken, Long installationId) {
    try {
      apiRestClient
          .get()
          .uri("/user/installations/{installationId}/repositories?per_page=1", installationId)
          .header(HttpHeaders.ACCEPT, "application/json")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
          .header("X-GitHub-Api-Version", API_VERSION)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (RestClientResponseException ex) {
      return false;
    }
  }

  @Override
  public GithubInstallationMetadata fetchInstallation(Long installationId) {
    GithubInstallationResponse githubInstallationResponse =
        apiRestClient
            .get()
            .uri("/app/installations/{installationId}", installationId)
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubAppJwtService.createJwt())
            .header("X-GitHub-Api-Version", API_VERSION)
            .retrieve()
            .body(GithubInstallationResponse.class);
    if (githubInstallationResponse == null || githubInstallationResponse.account() == null) {
      throw new IllegalStateException("GitHub installation lookup returned no installation");
    }
    return new GithubInstallationMetadata(
        githubInstallationResponse.id(),
        githubInstallationResponse.account().id(),
        githubInstallationResponse.account().login(),
        "Organization".equalsIgnoreCase(githubInstallationResponse.account().type())
            ? GithubInstallationAccountType.ORGANIZATION
            : GithubInstallationAccountType.USER,
        Instant.parse(githubInstallationResponse.createdAt()));
  }

  @Override
  public List<GithubRepositoryMetadata> listInstallationRepositories(Long installationId) {
    String installationAccessToken = createInstallationAccessToken(installationId);

    GithubInstallationRepositoriesResponse githubInstallationRepositoriesResponse =
        apiRestClient
            .get()
            .uri("/installation/repositories")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationAccessToken)
            .header("X-GitHub-Api-Version", API_VERSION)
            .retrieve()
            .body(GithubInstallationRepositoriesResponse.class);
    if (githubInstallationRepositoriesResponse == null
        || githubInstallationRepositoriesResponse.repositories() == null) {
      throw new IllegalStateException("GitHub installation repositories lookup returned no data");
    }
    return githubInstallationRepositoriesResponse.repositories().stream()
        .map(
            repository ->
                new GithubRepositoryMetadata(
                    repository.id(), repository.name(), repository.fullName()))
        .toList();
  }

  @Override
  public void upsertCheckRun(
      Long installationId, String repositoryFullName, GithubCheckRunUpsertCommand command) {
    String installationAccessToken = createInstallationAccessToken(installationId);
    String[] repositoryCoordinates = repositoryCoordinates(repositoryFullName);
    String owner = repositoryCoordinates[0];
    String repository = repositoryCoordinates[1];

    GithubCheckRunsResponse githubCheckRunsResponse =
        apiRestClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/repos/{owner}/{repo}/commits/{ref}/check-runs")
                        .queryParam("check_name", command.checkName())
                        .queryParam("filter", "latest")
                        .queryParam("per_page", 1)
                        .build(owner, repository, command.headSha()))
            .header(HttpHeaders.ACCEPT, GITHUB_JSON_MEDIA_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationAccessToken)
            .header("X-GitHub-Api-Version", API_VERSION)
            .retrieve()
            .body(GithubCheckRunsResponse.class);
    Long existingCheckRunId =
        githubCheckRunsResponse == null || githubCheckRunsResponse.checkRuns() == null
            ? null
            : githubCheckRunsResponse.checkRuns().stream()
                .map(GithubCheckRunResponse::id)
                .findFirst()
                .orElse(null);

    if (existingCheckRunId == null) {
      apiRestClient
          .post()
          .uri("/repos/{owner}/{repo}/check-runs", owner, repository)
          .header(HttpHeaders.ACCEPT, GITHUB_JSON_MEDIA_TYPE)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationAccessToken)
          .header("X-GitHub-Api-Version", API_VERSION)
          .body(
              new GithubCreateCheckRunRequest(
                  command.checkName(),
                  command.headSha(),
                  command.externalId(),
                  command.status(),
                  command.conclusion(),
                  command.startedAt(),
                  command.completedAt(),
                  new GithubCheckRunOutput(command.title(), command.summary(), command.text())))
          .retrieve()
          .toBodilessEntity();
      return;
    }

    apiRestClient
        .patch()
        .uri("/repos/{owner}/{repo}/check-runs/{checkRunId}", owner, repository, existingCheckRunId)
        .header(HttpHeaders.ACCEPT, GITHUB_JSON_MEDIA_TYPE)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationAccessToken)
        .header("X-GitHub-Api-Version", API_VERSION)
        .body(
            new GithubUpdateCheckRunRequest(
                command.checkName(),
                command.status(),
                command.conclusion(),
                command.startedAt(),
                command.completedAt(),
                new GithubCheckRunOutput(command.title(), command.summary(), command.text())))
        .retrieve()
        .toBodilessEntity();
  }

  private String createInstallationAccessToken(Long installationId) {
    GithubInstallationTokenResponse githubInstallationTokenResponse =
        apiRestClient
            .post()
            .uri("/app/installations/{installationId}/access_tokens", installationId)
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubAppJwtService.createJwt())
            .header("X-GitHub-Api-Version", API_VERSION)
            .retrieve()
            .body(GithubInstallationTokenResponse.class);
    if (githubInstallationTokenResponse == null
        || githubInstallationTokenResponse.token() == null) {
      throw new IllegalStateException("GitHub installation token lookup returned no token");
    }
    return githubInstallationTokenResponse.token();
  }

  private String[] repositoryCoordinates(String repositoryFullName) {
    String[] repositoryCoordinates = repositoryFullName.split("/", 2);
    if (repositoryCoordinates.length != 2
        || repositoryCoordinates[0].isBlank()
        || repositoryCoordinates[1].isBlank()) {
      throw new IllegalArgumentException("Invalid repository full name: " + repositoryFullName);
    }
    return repositoryCoordinates;
  }

  record GithubAccessTokenResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {}

  record GithubUserResponse(Long id, String login) {}

  record GithubInstallationResponse(
      Long id,
      GithubAccountResponse account,
      @com.fasterxml.jackson.annotation.JsonProperty("created_at") String createdAt) {}

  record GithubAccountResponse(Long id, String login, String type) {}

  record GithubInstallationTokenResponse(String token) {}

  record GithubInstallationRepositoriesResponse(List<GithubRepositoryResponse> repositories) {}

  record GithubRepositoryResponse(
      Long id,
      String name,
      @com.fasterxml.jackson.annotation.JsonProperty("full_name") String fullName) {}

  record GithubCheckRunsResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("check_runs")
          List<GithubCheckRunResponse> checkRuns) {}

  record GithubCheckRunResponse(Long id) {}

  record GithubCreateCheckRunRequest(
      String name,
      @com.fasterxml.jackson.annotation.JsonProperty("head_sha") String headSha,
      @com.fasterxml.jackson.annotation.JsonProperty("external_id") String externalId,
      String status,
      String conclusion,
      @com.fasterxml.jackson.annotation.JsonProperty("started_at") Instant startedAt,
      @com.fasterxml.jackson.annotation.JsonProperty("completed_at") Instant completedAt,
      GithubCheckRunOutput output) {}

  record GithubUpdateCheckRunRequest(
      String name,
      String status,
      String conclusion,
      @com.fasterxml.jackson.annotation.JsonProperty("started_at") Instant startedAt,
      @com.fasterxml.jackson.annotation.JsonProperty("completed_at") Instant completedAt,
      GithubCheckRunOutput output) {}

  record GithubCheckRunOutput(String title, String summary, String text) {}
}

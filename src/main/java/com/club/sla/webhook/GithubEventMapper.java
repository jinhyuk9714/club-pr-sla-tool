package com.club.sla.webhook;

import com.club.sla.installation.GithubInstallationAccountType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GithubEventMapper {

  private final ObjectMapper objectMapper;

  public GithubEventMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Optional<GithubEvent> map(String eventType, String payload) {
    JsonNode root;
    try {
      root = objectMapper.readTree(payload);
    } catch (IOException ex) {
      return Optional.empty();
    }

    if ("pull_request".equals(eventType)) {
      return mapPullRequestEvent(root);
    }
    if ("pull_request_review".equals(eventType)) {
      return mapPullRequestReviewEvent(root);
    }
    if ("installation".equals(eventType)) {
      return mapInstallationEvent(root);
    }
    if ("installation_repositories".equals(eventType)) {
      return mapInstallationRepositoriesEvent(root);
    }
    return Optional.empty();
  }

  private Optional<GithubEvent> mapPullRequestEvent(JsonNode root) {
    Optional<String> action = readText(root, "/action");
    Optional<Long> installationId = readLong(root, "/installation/id");
    Optional<Long> repositoryId = readLong(root, "/repository/id");
    Optional<Long> prNumber = readLong(root, "/pull_request/number");
    Optional<String> authorLogin = readText(root, "/pull_request/user/login");
    Optional<String> headSha = readText(root, "/pull_request/head/sha");

    if (action.isEmpty()
        || installationId.isEmpty()
        || repositoryId.isEmpty()
        || prNumber.isEmpty()
        || authorLogin.isEmpty()) {
      return Optional.empty();
    }

    if ("ready_for_review".equals(action.get())) {
      return Optional.of(
          new ReadyForReviewEvent(
              installationId.get(),
              repositoryId.get(),
              prNumber.get(),
              authorLogin.get(),
              headSha.orElse(null)));
    }
    if ("converted_to_draft".equals(action.get())) {
      return Optional.of(
          new ConvertedToDraftEvent(
              installationId.get(), repositoryId.get(), prNumber.get(), headSha.orElse(null)));
    }

    return Optional.empty();
  }

  private Optional<GithubEvent> mapPullRequestReviewEvent(JsonNode root) {
    Optional<String> action = readText(root, "/action");
    Optional<Long> installationId = readLong(root, "/installation/id");
    Optional<Long> repositoryId = readLong(root, "/repository/id");
    Optional<Long> prNumber = readLong(root, "/pull_request/number");
    Optional<String> reviewerLogin = readText(root, "/review/user/login");
    Optional<String> headSha = readText(root, "/pull_request/head/sha");

    if (action.isEmpty()
        || installationId.isEmpty()
        || repositoryId.isEmpty()
        || prNumber.isEmpty()
        || reviewerLogin.isEmpty()) {
      return Optional.empty();
    }
    if (!"submitted".equals(action.get())) {
      return Optional.empty();
    }

    return Optional.of(
        new ReviewSubmittedEvent(
            installationId.get(),
            repositoryId.get(),
            prNumber.get(),
            reviewerLogin.get(),
            headSha.orElse(null)));
  }

  private Optional<GithubEvent> mapInstallationEvent(JsonNode root) {
    Optional<String> action = readText(root, "/action");
    Optional<Long> installationId = readLong(root, "/installation/id");
    if (action.isEmpty() || installationId.isEmpty()) {
      return Optional.empty();
    }

    if ("created".equals(action.get()) || "unsuspend".equals(action.get())) {
      Optional<Long> accountId = readLong(root, "/installation/account/id");
      Optional<String> accountLogin = readText(root, "/installation/account/login");
      Optional<String> accountType = readText(root, "/installation/account/type");
      Optional<String> createdAt = readText(root, "/installation/created_at");
      if (accountId.isEmpty()
          || accountLogin.isEmpty()
          || accountType.isEmpty()
          || createdAt.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(
          new InstallationCreatedEvent(
              installationId.get(),
              accountId.get(),
              accountLogin.get(),
              "Organization".equalsIgnoreCase(accountType.get())
                  ? GithubInstallationAccountType.ORGANIZATION
                  : GithubInstallationAccountType.USER,
              Instant.parse(createdAt.get()),
              readRepositories(root, "/repositories")));
    }
    if ("deleted".equals(action.get()) || "suspend".equals(action.get())) {
      return Optional.of(new InstallationRemovedEvent(installationId.get()));
    }
    return Optional.empty();
  }

  private Optional<GithubEvent> mapInstallationRepositoriesEvent(JsonNode root) {
    Optional<Long> installationId = readLong(root, "/installation/id");
    if (installationId.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new InstallationRepositoriesChangedEvent(
            installationId.get(),
            readRepositories(root, "/repositories_added"),
            readRepositoryIds(root, "/repositories_removed")));
  }

  private List<InstallationRepositoryPayload> readRepositories(JsonNode root, String pointer) {
    JsonNode nodes = root.at(pointer);
    if (!nodes.isArray()) {
      return List.of();
    }
    List<InstallationRepositoryPayload> repositories = new ArrayList<>();
    for (JsonNode node : nodes) {
      if (node.path("id").canConvertToLong()
          && node.path("name").isTextual()
          && node.path("full_name").isTextual()) {
        repositories.add(
            new InstallationRepositoryPayload(
                node.path("id").longValue(),
                node.path("name").asText(),
                node.path("full_name").asText()));
      }
    }
    return repositories;
  }

  private List<Long> readRepositoryIds(JsonNode root, String pointer) {
    JsonNode nodes = root.at(pointer);
    if (!nodes.isArray()) {
      return List.of();
    }
    List<Long> repositoryIds = new ArrayList<>();
    for (JsonNode node : nodes) {
      if (node.path("id").canConvertToLong()) {
        repositoryIds.add(node.path("id").longValue());
      }
    }
    return repositoryIds;
  }

  private Optional<String> readText(JsonNode root, String pointer) {
    JsonNode node = root.at(pointer);
    if (node.isMissingNode() || node.isNull()) {
      return Optional.empty();
    }
    String value = node.asText();
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  private Optional<Long> readLong(JsonNode root, String pointer) {
    JsonNode node = root.at(pointer);
    if (node.isMissingNode() || node.isNull() || !node.canConvertToLong()) {
      return Optional.empty();
    }
    return Optional.of(node.longValue());
  }

  public sealed interface GithubEvent
      permits ReadyForReviewEvent,
          ConvertedToDraftEvent,
          ReviewSubmittedEvent,
          InstallationCreatedEvent,
          InstallationRemovedEvent,
          InstallationRepositoriesChangedEvent {}

  public record ReadyForReviewEvent(
      Long installationId, Long repositoryId, Long prNumber, String authorLogin, String headSha)
      implements GithubEvent {}

  public record ConvertedToDraftEvent(
      Long installationId, Long repositoryId, Long prNumber, String headSha)
      implements GithubEvent {}

  public record ReviewSubmittedEvent(
      Long installationId, Long repositoryId, Long prNumber, String reviewerLogin, String headSha)
      implements GithubEvent {}

  public record InstallationCreatedEvent(
      Long installationId,
      Long accountId,
      String accountLogin,
      GithubInstallationAccountType accountType,
      Instant installedAt,
      List<InstallationRepositoryPayload> repositories)
      implements GithubEvent {}

  public record InstallationRemovedEvent(Long installationId) implements GithubEvent {}

  public record InstallationRepositoriesChangedEvent(
      Long installationId,
      List<InstallationRepositoryPayload> repositoriesAdded,
      List<Long> repositoriesRemoved)
      implements GithubEvent {}

  public record InstallationRepositoryPayload(
      Long repositoryId, String repositoryName, String repositoryFullName) {}
}

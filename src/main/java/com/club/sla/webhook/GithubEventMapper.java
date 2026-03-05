package com.club.sla.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
    return Optional.empty();
  }

  private Optional<GithubEvent> mapPullRequestEvent(JsonNode root) {
    Optional<String> action = readText(root, "/action");
    Optional<Long> repositoryId = readLong(root, "/repository/id");
    Optional<Long> prNumber = readLong(root, "/pull_request/number");
    Optional<String> authorLogin = readText(root, "/pull_request/user/login");

    if (action.isEmpty() || repositoryId.isEmpty() || prNumber.isEmpty() || authorLogin.isEmpty()) {
      return Optional.empty();
    }

    if ("ready_for_review".equals(action.get())) {
      return Optional.of(
          new ReadyForReviewEvent(repositoryId.get(), prNumber.get(), authorLogin.get()));
    }
    if ("converted_to_draft".equals(action.get())) {
      return Optional.of(new ConvertedToDraftEvent(repositoryId.get(), prNumber.get()));
    }

    return Optional.empty();
  }

  private Optional<GithubEvent> mapPullRequestReviewEvent(JsonNode root) {
    Optional<String> action = readText(root, "/action");
    Optional<Long> repositoryId = readLong(root, "/repository/id");
    Optional<Long> prNumber = readLong(root, "/pull_request/number");
    Optional<String> reviewerLogin = readText(root, "/review/user/login");

    if (action.isEmpty()
        || repositoryId.isEmpty()
        || prNumber.isEmpty()
        || reviewerLogin.isEmpty()) {
      return Optional.empty();
    }
    if (!"submitted".equals(action.get())) {
      return Optional.empty();
    }

    return Optional.of(
        new ReviewSubmittedEvent(repositoryId.get(), prNumber.get(), reviewerLogin.get()));
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
      permits ReadyForReviewEvent, ConvertedToDraftEvent, ReviewSubmittedEvent {}

  public record ReadyForReviewEvent(Long repositoryId, Long prNumber, String authorLogin)
      implements GithubEvent {}

  public record ConvertedToDraftEvent(Long repositoryId, Long prNumber) implements GithubEvent {}

  public record ReviewSubmittedEvent(Long repositoryId, Long prNumber, String reviewerLogin)
      implements GithubEvent {}
}

package com.club.sla.github;

import java.util.List;

public interface GithubAppClient {

  GithubAuthenticatedUser exchangeCodeForUser(String code, String redirectUri);

  List<GithubInstallationMetadata> listUserInstallations(String userAccessToken);

  boolean userCanAccessInstallation(String userAccessToken, Long installationId);

  GithubInstallationMetadata fetchInstallation(Long installationId);

  List<GithubRepositoryMetadata> listInstallationRepositories(Long installationId);

  void upsertCheckRun(
      Long installationId, String repositoryFullName, GithubCheckRunUpsertCommand command);
}

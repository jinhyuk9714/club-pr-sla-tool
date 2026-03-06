package com.club.sla.installation;

public record GithubInstallationRepositoryEntryUpsertCommand(
    Long repositoryId, String repositoryName, String repositoryFullName) {}

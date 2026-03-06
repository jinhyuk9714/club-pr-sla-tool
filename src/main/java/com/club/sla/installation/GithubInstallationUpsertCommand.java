package com.club.sla.installation;

import java.time.Instant;

public record GithubInstallationUpsertCommand(
    Long installationId,
    Long accountId,
    String accountLogin,
    GithubInstallationAccountType accountType,
    Instant installedAt) {}

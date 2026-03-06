package com.club.sla.github;

import com.club.sla.installation.GithubInstallationAccountType;
import java.time.Instant;

public record GithubInstallationMetadata(
    Long installationId,
    Long accountId,
    String accountLogin,
    GithubInstallationAccountType accountType,
    Instant installedAt) {}

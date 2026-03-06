package com.club.sla.ops;

import com.club.sla.installation.GithubInstallationStatus;
import java.time.Instant;

public record OpsInstallationSummaryView(
    Long installationId,
    String accountLogin,
    GithubInstallationStatus status,
    boolean configured,
    long activeRepositoryCount,
    Instant updatedAt) {}

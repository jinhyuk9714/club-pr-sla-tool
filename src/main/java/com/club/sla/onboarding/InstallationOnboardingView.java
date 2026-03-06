package com.club.sla.onboarding;

import java.util.List;

public record InstallationOnboardingView(
    Long installationId,
    String accountLogin,
    List<String> repositoryFullNames,
    boolean configured) {}

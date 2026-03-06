package com.club.sla.onboarding;

import com.club.sla.github.GithubAuthenticatedUser;

public interface InstallationOnboardingService {

  boolean userCanAccessInstallation(GithubAuthenticatedUser authenticatedUser, Long installationId);

  InstallationOnboardingView loadInstallationView(Long installationId);

  void saveSettings(Long installationId, String discordWebhookUrl);
}

package com.club.sla.onboarding;

import com.club.sla.github.GithubAuthenticatedUser;
import java.util.List;

public interface InstallationOnboardingService {

  List<InstallationOnboardingView> listAccessibleInstallations(
      GithubAuthenticatedUser authenticatedUser);

  boolean userCanAccessInstallation(GithubAuthenticatedUser authenticatedUser, Long installationId);

  InstallationOnboardingView loadInstallationView(Long installationId);

  void saveSettings(Long installationId, String discordWebhookUrl);
}

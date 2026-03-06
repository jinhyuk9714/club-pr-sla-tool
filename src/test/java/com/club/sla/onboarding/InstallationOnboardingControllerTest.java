package com.club.sla.onboarding;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.club.sla.github.GithubAuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InstallationOnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
class InstallationOnboardingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GithubUserSessionService githubUserSessionService;
  @MockBean private InstallationOnboardingService installationOnboardingService;

  @Test
  void redirectsAnonymousUserToGithubLoginFromSetupRoute() throws Exception {
    given(githubUserSessionService.currentUser()).willReturn(Optional.empty());

    mockMvc
        .perform(get("/app/installations/setup").param("installation_id", "7001"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/login/github?returnTo=%2Fapp%2Finstallations%2Fsetup%3Finstallation_id%3D7001"));
  }

  @Test
  void redirectsAuthorizedUserFromSetupRouteToSettingsPage() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubUserSessionService.currentUser()).willReturn(Optional.of(authenticatedUser));
    given(installationOnboardingService.userCanAccessInstallation(authenticatedUser, 7001L))
        .willReturn(true);

    mockMvc
        .perform(get("/app/installations/setup").param("installation_id", "7001"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/app/installations/7001"));
  }

  @Test
  void rendersOnboardingErrorPageWhenSetupAccessCheckFails() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubUserSessionService.currentUser()).willReturn(Optional.of(authenticatedUser));
    given(installationOnboardingService.userCanAccessInstallation(authenticatedUser, 7001L))
        .willReturn(false);

    mockMvc
        .perform(get("/app/installations/setup").param("installation_id", "7001"))
        .andExpect(status().isOk())
        .andExpect(view().name("onboarding-error"))
        .andExpect(model().attribute("title", "설치 설정을 계속할 수 없습니다"))
        .andExpect(model().attribute("message", "설치 권한을 다시 확인한 뒤 로그인부터 다시 시도하세요."));
  }

  @Test
  void rendersSettingsPageForAuthorizedUser() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    InstallationOnboardingView installationOnboardingView =
        new InstallationOnboardingView(
            7001L, "club-org", List.of("club-org/club-pr-tool", "club-org/club-web"), false);
    given(githubUserSessionService.currentUser()).willReturn(Optional.of(authenticatedUser));
    given(installationOnboardingService.userCanAccessInstallation(authenticatedUser, 7001L))
        .willReturn(true);
    given(installationOnboardingService.loadInstallationView(7001L))
        .willReturn(installationOnboardingView);

    mockMvc
        .perform(get("/app/installations/7001"))
        .andExpect(status().isOk())
        .andExpect(view().name("installation-settings"))
        .andExpect(model().attribute("installation", installationOnboardingView));
  }

  @Test
  void rejectsSettingsPageWhenInstallationAccessCheckFails() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubUserSessionService.currentUser()).willReturn(Optional.of(authenticatedUser));
    given(installationOnboardingService.userCanAccessInstallation(authenticatedUser, 7001L))
        .willReturn(false);

    mockMvc.perform(get("/app/installations/7001")).andExpect(status().isForbidden());
  }

  @Test
  void savesValidatedDiscordWebhookAndRedirects() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubUserSessionService.currentUser()).willReturn(Optional.of(authenticatedUser));
    given(installationOnboardingService.userCanAccessInstallation(authenticatedUser, 7001L))
        .willReturn(true);

    mockMvc
        .perform(
            post("/app/installations/7001/settings")
                .param("discordWebhookUrl", "https://discord.example/webhook"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/app/installations/7001?saved=1"));

    verify(installationOnboardingService).saveSettings(7001L, "https://discord.example/webhook");
  }

  @Test
  void redisplaysSettingsPageWhenWebhookValidationFails() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    InstallationOnboardingView installationOnboardingView =
        new InstallationOnboardingView(7001L, "club-org", List.of("club-org/club-pr-tool"), false);
    given(githubUserSessionService.currentUser()).willReturn(Optional.of(authenticatedUser));
    given(installationOnboardingService.userCanAccessInstallation(authenticatedUser, 7001L))
        .willReturn(true);
    given(installationOnboardingService.loadInstallationView(7001L))
        .willReturn(installationOnboardingView);
    org.mockito.Mockito.doThrow(
            new InvalidDiscordWebhookException("Discord webhook validation failed"))
        .when(installationOnboardingService)
        .saveSettings(7001L, "https://discord.example/webhook");

    mockMvc
        .perform(
            post("/app/installations/7001/settings")
                .param("discordWebhookUrl", "https://discord.example/webhook"))
        .andExpect(status().isOk())
        .andExpect(view().name("installation-settings"))
        .andExpect(model().attribute("installation", installationOnboardingView))
        .andExpect(model().attribute("error", "Discord webhook validation failed"));
  }
}

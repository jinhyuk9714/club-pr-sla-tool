package com.club.sla.onboarding;

import com.club.sla.github.GithubAuthenticatedUser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class InstallationOnboardingController {

  private final GithubUserSessionService githubUserSessionService;
  private final InstallationOnboardingService installationOnboardingService;

  public InstallationOnboardingController(
      GithubUserSessionService githubUserSessionService,
      InstallationOnboardingService installationOnboardingService) {
    this.githubUserSessionService = githubUserSessionService;
    this.installationOnboardingService = installationOnboardingService;
  }

  @GetMapping("/app/installations/setup")
  public RedirectView setup(@RequestParam("installation_id") Long installationId) {
    GithubAuthenticatedUser authenticatedUser = githubUserSessionService.currentUser().orElse(null);
    if (authenticatedUser == null) {
      return new RedirectView(
          loginRedirectFor("/app/installations/setup?installation_id=" + installationId));
    }
    if (!installationOnboardingService.userCanAccessInstallation(
        authenticatedUser, installationId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return new RedirectView("/app/installations/" + installationId);
  }

  @GetMapping("/app/installations/{installationId}")
  public String settings(@PathVariable Long installationId, Model model) {
    GithubAuthenticatedUser authenticatedUser = githubUserSessionService.currentUser().orElse(null);
    if (authenticatedUser == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    if (!installationOnboardingService.userCanAccessInstallation(
        authenticatedUser, installationId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    model.addAttribute(
        "installation", installationOnboardingService.loadInstallationView(installationId));
    return "installation-settings";
  }

  @PostMapping("/app/installations/{installationId}/settings")
  public String saveSettings(
      @PathVariable Long installationId, @RequestParam String discordWebhookUrl, Model model) {
    GithubAuthenticatedUser authenticatedUser = githubUserSessionService.currentUser().orElse(null);
    if (authenticatedUser == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    if (!installationOnboardingService.userCanAccessInstallation(
        authenticatedUser, installationId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    try {
      installationOnboardingService.saveSettings(installationId, discordWebhookUrl);
      return "redirect:/app/installations/" + installationId + "?saved=1";
    } catch (InvalidDiscordWebhookException ex) {
      model.addAttribute(
          "installation", installationOnboardingService.loadInstallationView(installationId));
      model.addAttribute("error", ex.getMessage());
      return "installation-settings";
    }
  }

  private String loginRedirectFor(String returnTo) {
    return "/login/github?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
  }
}

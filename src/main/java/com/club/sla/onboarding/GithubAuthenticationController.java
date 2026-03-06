package com.club.sla.onboarding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class GithubAuthenticationController {

  private final GithubAuthenticationService githubAuthenticationService;
  private final GithubUserSessionService githubUserSessionService;
  private final String installUrl;

  public GithubAuthenticationController(
      GithubAuthenticationService githubAuthenticationService,
      GithubUserSessionService githubUserSessionService,
      @Value("${github.app.install-url:https://github.com/apps/club-pr-sla/installations/new}")
          String installUrl) {
    this.githubAuthenticationService = githubAuthenticationService;
    this.githubUserSessionService = githubUserSessionService;
    this.installUrl = installUrl;
  }

  @GetMapping("/login/github")
  public RedirectView login(@RequestParam(defaultValue = "/") String returnTo) {
    return new RedirectView(githubAuthenticationService.createAuthorizationRedirectUrl(returnTo));
  }

  @GetMapping("/auth/github/callback")
  public Object callback(@RequestParam String code, @RequestParam String state, Model model) {
    try {
      GithubAuthenticationResult githubAuthenticationResult =
          githubAuthenticationService.authenticate(code, state);
      githubUserSessionService.storeAuthenticatedUser(
          githubAuthenticationResult.authenticatedUser());
      return new RedirectView(githubAuthenticationResult.redirectPath());
    } catch (RuntimeException ex) {
      model.addAttribute("title", "GitHub 로그인 실패");
      model.addAttribute("message", "다시 로그인하거나 GitHub App 설치부터 다시 시작하세요.");
      model.addAttribute("loginUrl", "/login/github?returnTo=%2F");
      model.addAttribute("installUrl", installUrl);
      return "onboarding-error";
    }
  }
}

package com.club.sla.onboarding;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class GithubAuthenticationController {

  private final GithubAuthenticationService githubAuthenticationService;
  private final GithubUserSessionService githubUserSessionService;

  public GithubAuthenticationController(
      GithubAuthenticationService githubAuthenticationService,
      GithubUserSessionService githubUserSessionService) {
    this.githubAuthenticationService = githubAuthenticationService;
    this.githubUserSessionService = githubUserSessionService;
  }

  @GetMapping("/login/github")
  public RedirectView login(@RequestParam(defaultValue = "/") String returnTo) {
    return new RedirectView(githubAuthenticationService.createAuthorizationRedirectUrl(returnTo));
  }

  @GetMapping("/auth/github/callback")
  public RedirectView callback(@RequestParam String code, @RequestParam String state) {
    GithubAuthenticationResult githubAuthenticationResult =
        githubAuthenticationService.authenticate(code, state);
    githubUserSessionService.storeAuthenticatedUser(githubAuthenticationResult.authenticatedUser());
    return new RedirectView(githubAuthenticationResult.redirectPath());
  }
}

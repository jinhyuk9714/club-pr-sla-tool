package com.club.sla.onboarding;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.club.sla.github.GithubAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GithubAuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GithubAuthenticationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GithubAuthenticationService githubAuthenticationService;
  @MockBean private GithubUserSessionService githubUserSessionService;

  @Test
  void redirectsToGithubLoginUrl() throws Exception {
    given(
            githubAuthenticationService.createAuthorizationRedirectUrl(
                "/app/installations/setup?installation_id=7001"))
        .willReturn("https://github.com/login/oauth/authorize?state=test-state");

    mockMvc
        .perform(
            get("/login/github").param("returnTo", "/app/installations/setup?installation_id=7001"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("https://github.com/login/oauth/authorize?state=test-state"));
  }

  @Test
  void storesAuthenticatedUserAndRedirectsToOriginalLocation() throws Exception {
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(githubAuthenticationService.authenticate("oauth-code", "state-1"))
        .willReturn(
            new GithubAuthenticationResult(
                authenticatedUser, "/app/installations/setup?installation_id=7001"));

    mockMvc
        .perform(get("/auth/github/callback").param("code", "oauth-code").param("state", "state-1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/app/installations/setup?installation_id=7001"));

    verify(githubUserSessionService).storeAuthenticatedUser(authenticatedUser);
  }

  @Test
  void rendersOnboardingErrorPageWhenCallbackFails() throws Exception {
    given(githubAuthenticationService.authenticate("oauth-code", "state-1"))
        .willThrow(new IllegalArgumentException("Invalid GitHub login state"));

    mockMvc
        .perform(get("/auth/github/callback").param("code", "oauth-code").param("state", "state-1"))
        .andExpect(status().isOk())
        .andExpect(view().name("onboarding-error"))
        .andExpect(model().attribute("title", "GitHub 로그인 실패"))
        .andExpect(model().attribute("message", "다시 로그인하거나 GitHub App 설치부터 다시 시작하세요."));
  }
}

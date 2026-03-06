package com.club.sla.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.club.sla.github.GithubAppClient;
import com.club.sla.github.GithubAuthenticatedUser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class HttpSessionGithubAuthenticationServiceTest {

  @Mock private GithubAppClient githubAppClient;

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void createsGithubAuthorizationUrlAndStoresReturnPathInSession() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/github");
    request.setScheme("https");
    request.setServerName("club-pr.example");
    request.setServerPort(443);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    HttpSessionGithubAuthenticationService service =
        new HttpSessionGithubAuthenticationService(githubAppClient, "client-123");

    String authorizationUrl =
        service.createAuthorizationRedirectUrl("/app/installations/setup?installation_id=7001");

    assertThat(authorizationUrl)
        .contains("client_id=client-123")
        .contains("redirect_uri=https%3A%2F%2Fclub-pr.example%2Fauth%2Fgithub%2Fcallback")
        .contains("state=");
    assertThat(request.getSession().getAttribute("githubLoginReturnTo"))
        .isEqualTo("/app/installations/setup?installation_id=7001");
  }

  @Test
  void authenticatesCodeAndReturnsStoredRedirectPath() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/github");
    request.setScheme("https");
    request.setServerName("club-pr.example");
    request.setServerPort(443);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    HttpSessionGithubAuthenticationService service =
        new HttpSessionGithubAuthenticationService(githubAppClient, "client-123");
    String authorizationUrl =
        service.createAuthorizationRedirectUrl("/app/installations/setup?installation_id=7001");
    String state = extractState(authorizationUrl);
    GithubAuthenticatedUser authenticatedUser =
        new GithubAuthenticatedUser(101L, "alice", "user-token");
    given(
            githubAppClient.exchangeCodeForUser(
                "oauth-code", "https://club-pr.example/auth/github/callback"))
        .willReturn(authenticatedUser);

    GithubAuthenticationResult githubAuthenticationResult =
        service.authenticate("oauth-code", state);

    assertThat(githubAuthenticationResult.authenticatedUser()).isEqualTo(authenticatedUser);
    assertThat(githubAuthenticationResult.redirectPath())
        .isEqualTo("/app/installations/setup?installation_id=7001");
  }

  @Test
  void usesForwardedHeadersWhenBuildingGithubCallbackUrl() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/github");
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8081);
    request.addHeader("X-Forwarded-Proto", "https");
    request.addHeader("X-Forwarded-Host", "cytoplastic-cheerily-maximus.ngrok-free.dev");
    request.addHeader("X-Forwarded-Port", "443");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    HttpSessionGithubAuthenticationService service =
        new HttpSessionGithubAuthenticationService(githubAppClient, "client-123");

    String authorizationUrl = service.createAuthorizationRedirectUrl("/");

    assertThat(authorizationUrl)
        .contains(
            "redirect_uri=https%3A%2F%2Fcytoplastic-cheerily-maximus.ngrok-free.dev%2Fauth%2Fgithub%2Fcallback");
  }

  private String extractState(String authorizationUrl) {
    Matcher matcher = Pattern.compile("state=([^&]+)").matcher(authorizationUrl);
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }
}

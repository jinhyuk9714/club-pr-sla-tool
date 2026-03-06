package com.club.sla.onboarding;

import com.club.sla.github.GithubAppClient;
import com.club.sla.github.GithubAuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class HttpSessionGithubAuthenticationService implements GithubAuthenticationService {

  private static final String STATE_SESSION_KEY = "githubLoginState";
  private static final String RETURN_TO_SESSION_KEY = "githubLoginReturnTo";

  private final GithubAppClient githubAppClient;
  private final String clientId;

  public HttpSessionGithubAuthenticationService(
      GithubAppClient githubAppClient, @Value("${github.app.client-id:}") String clientId) {
    this.githubAppClient = githubAppClient;
    this.clientId = clientId;
  }

  @Override
  public String createAuthorizationRedirectUrl(String returnTo) {
    HttpSession session = currentSession();
    String state = UUID.randomUUID().toString();
    session.setAttribute(STATE_SESSION_KEY, state);
    session.setAttribute(RETURN_TO_SESSION_KEY, returnTo);
    String redirectUri = callbackUrl();
    return "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&state=%s"
        .formatted(urlEncode(clientId), urlEncode(redirectUri), urlEncode(state));
  }

  @Override
  public GithubAuthenticationResult authenticate(String code, String state) {
    HttpSession session = currentSession();
    String expectedState = (String) session.getAttribute(STATE_SESSION_KEY);
    if (expectedState == null || !expectedState.equals(state)) {
      throw new IllegalArgumentException("Invalid GitHub login state");
    }

    GithubAuthenticatedUser authenticatedUser =
        githubAppClient.exchangeCodeForUser(code, callbackUrl());
    String redirectPath = (String) session.getAttribute(RETURN_TO_SESSION_KEY);
    session.removeAttribute(STATE_SESSION_KEY);
    session.removeAttribute(RETURN_TO_SESSION_KEY);
    return new GithubAuthenticationResult(
        authenticatedUser, redirectPath == null || redirectPath.isBlank() ? "/" : redirectPath);
  }

  private HttpSession currentSession() {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes == null) {
      throw new IllegalStateException("No current request is available");
    }
    return requestAttributes.getRequest().getSession(true);
  }

  private String callbackUrl() {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes == null) {
      throw new IllegalStateException("No current request is available");
    }
    HttpServletRequest request = requestAttributes.getRequest();
    String scheme = firstHeaderValue(request, "X-Forwarded-Proto");
    if (scheme == null || scheme.isBlank()) {
      scheme = request.getScheme();
    }

    String forwardedHost = firstHeaderValue(request, "X-Forwarded-Host");
    String host = request.getServerName();
    Integer port = request.getServerPort();
    if (forwardedHost != null && !forwardedHost.isBlank()) {
      String normalizedForwardedHost = forwardedHost.trim();
      int colonIndex = normalizedForwardedHost.lastIndexOf(':');
      if (colonIndex > 0) {
        host = normalizedForwardedHost.substring(0, colonIndex);
        port = parsePort(normalizedForwardedHost.substring(colonIndex + 1), port);
      } else {
        host = normalizedForwardedHost;
      }
    }

    String forwardedPort = firstHeaderValue(request, "X-Forwarded-Port");
    if (forwardedPort != null && !forwardedPort.isBlank()) {
      port = parsePort(forwardedPort, port);
    }

    return scheme
        + "://"
        + host
        + (port == 80 || port == 443 ? "" : ":" + port)
        + "/auth/github/callback";
  }

  private ServletRequestAttributes servletRequestAttributes() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
      return servletRequestAttributes;
    }
    return null;
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String firstHeaderValue(HttpServletRequest request, String headerName) {
    String value = request.getHeader(headerName);
    if (value == null || value.isBlank()) {
      return null;
    }
    int commaIndex = value.indexOf(',');
    return commaIndex >= 0 ? value.substring(0, commaIndex).trim() : value.trim();
  }

  private Integer parsePort(String value, Integer fallback) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }
}

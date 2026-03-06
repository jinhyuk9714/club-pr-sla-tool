package com.club.sla.onboarding;

public interface GithubAuthenticationService {

  String createAuthorizationRedirectUrl(String returnTo);

  GithubAuthenticationResult authenticate(String code, String state);
}

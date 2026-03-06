package com.club.sla.onboarding;

import com.club.sla.github.GithubAuthenticatedUser;

public record GithubAuthenticationResult(
    GithubAuthenticatedUser authenticatedUser, String redirectPath) {}

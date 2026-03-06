package com.club.sla.github;

public record GithubAuthenticatedUser(Long userId, String login, String accessToken) {}

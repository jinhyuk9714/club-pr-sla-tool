package com.club.sla.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BetaAccessPolicy {

  private final Set<String> allowedGithubLogins;
  private final Set<String> allowedGithubAccounts;

  public BetaAccessPolicy(
      @Value("${beta.access.allowed-logins:}") String allowedGithubLogins,
      @Value("${beta.access.allowed-accounts:}") String allowedGithubAccounts) {
    this.allowedGithubLogins = parseCsv(allowedGithubLogins);
    this.allowedGithubAccounts = parseCsv(allowedGithubAccounts);
  }

  public boolean isAllowed(String githubLogin, String installationAccountLogin) {
    return allowedGithubLogins.contains(normalize(githubLogin))
        || allowedGithubAccounts.contains(normalize(installationAccountLogin));
  }

  private Set<String> parseCsv(String csv) {
    return Arrays.stream(csv.split(","))
        .map(this::normalize)
        .filter(value -> !value.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }
}

package com.club.sla.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BetaAccessPolicyTest {

  @Test
  void allowsWhenGithubUserLoginIsAllowlisted() {
    BetaAccessPolicy betaAccessPolicy = new BetaAccessPolicy("alice,bob", "");

    assertThat(betaAccessPolicy.isAllowed("alice", "club-org")).isTrue();
  }

  @Test
  void allowsWhenInstallationAccountIsAllowlisted() {
    BetaAccessPolicy betaAccessPolicy = new BetaAccessPolicy("", "club-org,club-team");

    assertThat(betaAccessPolicy.isAllowed("carol", "club-team")).isTrue();
  }

  @Test
  void rejectsWhenNeitherUserNorInstallationAccountIsAllowlisted() {
    BetaAccessPolicy betaAccessPolicy = new BetaAccessPolicy("alice", "club-org");

    assertThat(betaAccessPolicy.isAllowed("dave", "other-org")).isFalse();
  }
}

package com.club.sla.onboarding;

import com.club.sla.github.GithubAuthenticatedUser;
import java.util.Optional;

public interface GithubUserSessionService {

  Optional<GithubAuthenticatedUser> currentUser();

  void storeAuthenticatedUser(GithubAuthenticatedUser authenticatedUser);

  void clear();
}

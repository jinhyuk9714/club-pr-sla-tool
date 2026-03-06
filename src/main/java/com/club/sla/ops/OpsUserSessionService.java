package com.club.sla.ops;

import java.util.Optional;

public interface OpsUserSessionService {

  Optional<OpsAuthenticatedSession> currentSession();

  void storeAuthenticatedSession(OpsAuthenticatedSession authenticatedSession);

  void clear();
}

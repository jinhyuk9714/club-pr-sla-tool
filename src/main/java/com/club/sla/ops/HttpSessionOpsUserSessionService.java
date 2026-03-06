package com.club.sla.ops;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class HttpSessionOpsUserSessionService implements OpsUserSessionService {

  public static final String AUTHENTICATED_OPS_SESSION_KEY = "opsAuthenticatedSession";

  @Override
  public Optional<OpsAuthenticatedSession> currentSession() {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes == null) {
      return Optional.empty();
    }
    Object value =
        requestAttributes.getRequest().getSession(true).getAttribute(AUTHENTICATED_OPS_SESSION_KEY);
    if (value instanceof OpsAuthenticatedSession authenticatedSession) {
      return Optional.of(authenticatedSession);
    }
    return Optional.empty();
  }

  @Override
  public void storeAuthenticatedSession(OpsAuthenticatedSession authenticatedSession) {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes == null) {
      throw new IllegalStateException("No current request is available for session storage");
    }
    requestAttributes
        .getRequest()
        .getSession(true)
        .setAttribute(AUTHENTICATED_OPS_SESSION_KEY, authenticatedSession);
  }

  @Override
  public void clear() {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes != null) {
      requestAttributes
          .getRequest()
          .getSession(true)
          .removeAttribute(AUTHENTICATED_OPS_SESSION_KEY);
    }
  }

  private ServletRequestAttributes servletRequestAttributes() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
      return servletRequestAttributes;
    }
    return null;
  }
}

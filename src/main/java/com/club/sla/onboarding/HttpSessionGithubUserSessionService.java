package com.club.sla.onboarding;

import com.club.sla.github.GithubAuthenticatedUser;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class HttpSessionGithubUserSessionService implements GithubUserSessionService {

  public static final String AUTHENTICATED_USER_SESSION_KEY = "githubAuthenticatedUser";

  @Override
  public Optional<GithubAuthenticatedUser> currentUser() {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes == null) {
      return Optional.empty();
    }
    Object value =
        requestAttributes
            .getRequest()
            .getSession(true)
            .getAttribute(AUTHENTICATED_USER_SESSION_KEY);
    if (value instanceof GithubAuthenticatedUser authenticatedUser) {
      return Optional.of(authenticatedUser);
    }
    return Optional.empty();
  }

  @Override
  public void storeAuthenticatedUser(GithubAuthenticatedUser authenticatedUser) {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes == null) {
      throw new IllegalStateException("No current request is available for session storage");
    }
    requestAttributes
        .getRequest()
        .getSession(true)
        .setAttribute(AUTHENTICATED_USER_SESSION_KEY, authenticatedUser);
  }

  @Override
  public void clear() {
    ServletRequestAttributes requestAttributes = servletRequestAttributes();
    if (requestAttributes != null) {
      requestAttributes
          .getRequest()
          .getSession(true)
          .removeAttribute(AUTHENTICATED_USER_SESSION_KEY);
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

package com.club.sla.security;

import com.club.sla.ops.HttpSessionOpsUserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OpsRouteSessionGuardFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (requiresOpsSession(request) && !hasOpsSession(request)) {
      response.sendRedirect("/ops/login");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean requiresOpsSession(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    if ("/ops/login".equals(requestUri)) {
      return false;
    }
    return "/ops".equals(requestUri) || requestUri.startsWith("/ops/");
  }

  private boolean hasOpsSession(HttpServletRequest request) {
    if (request.getSession(false) == null) {
      return false;
    }
    Object authenticatedSession =
        request
            .getSession(false)
            .getAttribute(HttpSessionOpsUserSessionService.AUTHENTICATED_OPS_SESSION_KEY);
    return authenticatedSession != null;
  }
}

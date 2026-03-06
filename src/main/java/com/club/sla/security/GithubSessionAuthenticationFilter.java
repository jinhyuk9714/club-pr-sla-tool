package com.club.sla.security;

import com.club.sla.github.GithubAuthenticatedUser;
import com.club.sla.onboarding.HttpSessionGithubUserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GithubSessionAuthenticationFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (request.getSession(false) != null) {
      Object authenticatedUser =
          request
              .getSession(false)
              .getAttribute(HttpSessionGithubUserSessionService.AUTHENTICATED_USER_SESSION_KEY);
      if (authenticatedUser instanceof GithubAuthenticatedUser githubAuthenticatedUser) {
        mergeAuthentication(githubAuthenticatedUser, "ROLE_GITHUB_USER");
      }
    }
    filterChain.doFilter(request, response);
  }

  private void mergeAuthentication(Object principal, String role) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    Object securityPrincipal = principal;
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      securityPrincipal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      authorities.addAll(SecurityContextHolder.getContext().getAuthentication().getAuthorities());
    }
    if (authorities.stream().noneMatch(authority -> authority.getAuthority().equals(role))) {
      authorities.add(new SimpleGrantedAuthority(role));
    }
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(securityPrincipal, null, authorities));
  }
}

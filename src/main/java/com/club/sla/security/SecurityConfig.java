package com.club.sla.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      OpsApiKeyFilter opsApiKeyFilter,
      OpsRouteSessionGuardFilter opsRouteSessionGuardFilter,
      GithubSessionAuthenticationFilter githubSessionAuthenticationFilter,
      OpsSessionAuthenticationFilter opsSessionAuthenticationFilter)
      throws Exception {
    HttpStatusEntryPoint unauthorizedEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    http.csrf(csrf -> csrf.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .logout(logout -> logout.disable())
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers(HttpMethod.GET, "/", "/login/github", "/auth/github/callback")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/app/installations/setup")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/ops/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/ops/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/health", "/actuator/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/webhooks/github")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST, "/api/repositories/*/pull-requests/*/sla/re-evaluate")
                    .permitAll()
                    .requestMatchers("/ops/**")
                    .hasRole("OPS_USER")
                    .requestMatchers("/app/installations/**")
                    .hasRole("GITHUB_USER")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .defaultAuthenticationEntryPointFor(
                        unauthorizedEntryPoint,
                        request -> request.getRequestURI().startsWith("/app/installations/"))
                    .authenticationEntryPoint(unauthorizedEntryPoint))
        .addFilterBefore(opsRouteSessionGuardFilter, AnonymousAuthenticationFilter.class)
        .addFilterBefore(opsSessionAuthenticationFilter, AnonymousAuthenticationFilter.class)
        .addFilterBefore(githubSessionAuthenticationFilter, AnonymousAuthenticationFilter.class)
        .addFilterBefore(opsApiKeyFilter, AnonymousAuthenticationFilter.class);
    return http.build();
  }
}

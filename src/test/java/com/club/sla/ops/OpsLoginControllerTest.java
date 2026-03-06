package com.club.sla.ops;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.club.sla.audit.AdminAuditLogService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpsLoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpsLoginControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OpsUiAuthenticationService opsUiAuthenticationService;
  @MockBean private OpsUserSessionService opsUserSessionService;
  @MockBean private AdminAuditLogService adminAuditLogService;

  @Test
  void rendersOpsLoginPage() throws Exception {
    given(opsUserSessionService.currentSession()).willReturn(Optional.empty());

    mockMvc
        .perform(get("/ops/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("ops-login"));
  }

  @Test
  void redirectsAuthenticatedOperatorAwayFromLoginPage() throws Exception {
    given(opsUserSessionService.currentSession())
        .willReturn(
            Optional.of(
                new OpsAuthenticatedSession("ops-admin", Instant.parse("2026-03-06T00:00:00Z"))));

    mockMvc
        .perform(get("/ops/login"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops"));
  }

  @Test
  void redirectsToOpsHomeWhenLoginSucceeds() throws Exception {
    given(opsUiAuthenticationService.authenticate("test-admin-key", "127.0.0.1"))
        .willReturn(OpsUiLoginResult.success());

    mockMvc
        .perform(
            post("/ops/login")
                .param("adminApiKey", "test-admin-key")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ops"))
        .andExpect(flash().attribute("successMessage", "Ops session opened."));

    verify(adminAuditLogService)
        .record("OPS_UI_LOGIN", "/ops/login", "POST", 200, "SUCCESS", null, null, null, null);
  }

  @Test
  void redisplaysLoginPageWhenApiKeyIsInvalid() throws Exception {
    given(opsUiAuthenticationService.authenticate("wrong-key", "127.0.0.1"))
        .willReturn(OpsUiLoginResult.invalidCredentials());

    mockMvc
        .perform(
            post("/ops/login")
                .param("adminApiKey", "wrong-key")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isUnauthorized())
        .andExpect(view().name("ops-login"))
        .andExpect(model().attribute("error", "Invalid admin API key."));

    verify(adminAuditLogService)
        .record(
            "OPS_UI_LOGIN",
            "/ops/login",
            "POST",
            401,
            "INVALID_CREDENTIALS",
            null,
            null,
            null,
            "Invalid admin API key.");
  }

  @Test
  void redisplaysLoginPageWithRetryAfterWhenRateLimited() throws Exception {
    given(opsUiAuthenticationService.authenticate("test-admin-key", "127.0.0.1"))
        .willReturn(OpsUiLoginResult.rateLimited(60));

    mockMvc
        .perform(
            post("/ops/login")
                .param("adminApiKey", "test-admin-key")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(view().name("ops-login"))
        .andExpect(model().attribute("error", "Too many login attempts. Try again later."));

    verify(adminAuditLogService)
        .record(
            "OPS_UI_LOGIN",
            "/ops/login",
            "POST",
            429,
            "RATE_LIMITED",
            null,
            null,
            null,
            "Too many login attempts. Try again later.");
  }
}

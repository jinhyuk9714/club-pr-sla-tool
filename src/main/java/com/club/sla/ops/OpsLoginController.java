package com.club.sla.ops;

import com.club.sla.audit.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OpsLoginController {

  private final OpsUiAuthenticationService opsUiAuthenticationService;
  private final OpsUserSessionService opsUserSessionService;
  private final AdminAuditLogService adminAuditLogService;

  public OpsLoginController(
      OpsUiAuthenticationService opsUiAuthenticationService,
      OpsUserSessionService opsUserSessionService,
      AdminAuditLogService adminAuditLogService) {
    this.opsUiAuthenticationService = opsUiAuthenticationService;
    this.opsUserSessionService = opsUserSessionService;
    this.adminAuditLogService = adminAuditLogService;
  }

  @GetMapping("/ops/login")
  public String loginPage() {
    if (opsUserSessionService.currentSession().isPresent()) {
      return "redirect:/ops";
    }
    return "ops-login";
  }

  @PostMapping("/ops/login")
  public String login(
      @RequestParam("adminApiKey") String adminApiKey,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirectAttributes,
      Model model) {
    OpsUiLoginResult result =
        opsUiAuthenticationService.authenticate(adminApiKey, request.getRemoteAddr());
    if (result.authenticated()) {
      adminAuditLogService.record(
          "OPS_UI_LOGIN",
          request.getRequestURI(),
          request.getMethod(),
          200,
          "SUCCESS",
          null,
          null,
          null,
          null);
      redirectAttributes.addFlashAttribute("successMessage", "Ops session opened.");
      return "redirect:/ops";
    }

    if (result.rateLimited()) {
      adminAuditLogService.record(
          "OPS_UI_LOGIN",
          request.getRequestURI(),
          request.getMethod(),
          429,
          "RATE_LIMITED",
          null,
          null,
          null,
          result.errorMessage());
      response.setStatus(429);
      response.setHeader("Retry-After", Long.toString(result.retryAfterSeconds()));
      model.addAttribute("error", result.errorMessage());
      return "ops-login";
    }

    adminAuditLogService.record(
        "OPS_UI_LOGIN",
        request.getRequestURI(),
        request.getMethod(),
        401,
        "INVALID_CREDENTIALS",
        null,
        null,
        null,
        result.errorMessage());
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    model.addAttribute("error", result.errorMessage());
    return "ops-login";
  }

  @PostMapping("/ops/logout")
  public String logout(RedirectAttributes redirectAttributes) {
    opsUiAuthenticationService.logout();
    redirectAttributes.addFlashAttribute("successMessage", "Ops session closed.");
    return "redirect:/ops/login";
  }
}

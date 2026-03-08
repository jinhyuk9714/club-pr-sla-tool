package com.club.sla.onboarding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

  private final String installUrl;

  public HomePageController(
      @Value("${github.app.install-url:https://github.com/apps/club-pr-sla/installations/new}")
          String installUrl) {
    this.installUrl = installUrl;
  }

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("installUrl", installUrl);
    model.addAttribute("resumeUrl", "/app/installations");
    return "home";
  }
}

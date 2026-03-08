package com.club.sla.onboarding;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomePageController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(
    properties = {"github.app.install-url=https://github.com/apps/club-pr-sla/installations/new"})
class HomePageControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void rendersHomePageWithInstallUrl() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(
            model()
                .attribute("installUrl", "https://github.com/apps/club-pr-sla/installations/new"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("GitHub App 설치")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("설치 후 설정 계속하기")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("href=\"/app/installations\"")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"installation_id\""))));
  }
}

package com.club.sla.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ApplicationHealthService applicationHealthService;

  @Test
  void returnsOkWhenDatabaseAndMigrationsAreHealthy() throws Exception {
    given(applicationHealthService.currentHealth())
        .willReturn(new ApplicationHealthSnapshot("UP", "UP", "UP"));

    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.database").value("UP"))
        .andExpect(jsonPath("$.migrations").value("UP"));
  }

  @Test
  void returnsServiceUnavailableWhenHealthCheckFails() throws Exception {
    given(applicationHealthService.currentHealth())
        .willReturn(new ApplicationHealthSnapshot("DOWN", "UP", "DOWN"));

    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"))
        .andExpect(jsonPath("$.database").value("UP"))
        .andExpect(jsonPath("$.migrations").value("DOWN"));
  }
}

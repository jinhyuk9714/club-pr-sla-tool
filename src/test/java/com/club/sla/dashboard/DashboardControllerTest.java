package com.club.sla.dashboard;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DashboardQueryService dashboardQueryService;

  @Test
  void returnsDashboardSummaryByRepository() throws Exception {
    given(dashboardQueryService.fetch(9001L)).willReturn(new DashboardSummaryDto(4, 2, 1));

    mockMvc
        .perform(get("/api/repositories/9001/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onTrack").value(4))
        .andExpect(jsonPath("$.atRisk").value(2))
        .andExpect(jsonPath("$.breached").value(1));
  }
}

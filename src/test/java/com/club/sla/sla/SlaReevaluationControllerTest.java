package com.club.sla.sla;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SlaReevaluationController.class)
class SlaReevaluationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private SlaReevaluationService slaReevaluationService;

  @Test
  void returnsOkWithResultBodyWhenReevaluationCompletes() throws Exception {
    given(slaReevaluationService.reevaluate(9001L, 77L))
        .willReturn(
            new SlaReevaluationResultDto(
                true,
                SlaAction.REMIND_12H,
                "ACTION_DISPATCHED",
                Instant.parse("2026-03-06T01:00:00Z")));

    mockMvc
        .perform(post("/api/repositories/9001/pull-requests/77/sla/re-evaluate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.actionDispatched").value(true))
        .andExpect(jsonPath("$.action").value("REMIND_12H"))
        .andExpect(jsonPath("$.reason").value("ACTION_DISPATCHED"));
  }

  @Test
  void returnsNotFoundWhenPullRequestDoesNotExist() throws Exception {
    given(slaReevaluationService.reevaluate(9001L, 999L))
        .willReturn(new SlaReevaluationResultDto(false, null, "NOT_FOUND", Instant.now()));

    mockMvc
        .perform(post("/api/repositories/9001/pull-requests/999/sla/re-evaluate"))
        .andExpect(status().isNotFound());
  }
}

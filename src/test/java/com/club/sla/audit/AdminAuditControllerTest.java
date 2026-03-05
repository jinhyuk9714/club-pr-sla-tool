package com.club.sla.audit;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuditControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminAuditLogService adminAuditLogService;

  @Test
  void returnsAuditLogsWithOperationFilterAndLimit() throws Exception {
    AdminAuditLogDto row =
        new AdminAuditLogDto(
            10L,
            "SLA_REEVALUATE",
            "/api/repositories/1/pull-requests/2/sla/re-evaluate",
            "POST",
            200,
            "SUCCESS",
            1L,
            2L,
            null,
            null,
            Instant.parse("2026-03-06T01:00:00Z"));
    given(adminAuditLogService.list("SLA_REEVALUATE", 20)).willReturn(List.of(row));

    mockMvc
        .perform(
            get("/api/admin/audit-logs").param("operation", "SLA_REEVALUATE").param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].operation").value("SLA_REEVALUATE"))
        .andExpect(jsonPath("$[0].httpStatus").value(200))
        .andExpect(jsonPath("$[0].repositoryId").value(1))
        .andExpect(jsonPath("$[0].prNumber").value(2));
  }
}

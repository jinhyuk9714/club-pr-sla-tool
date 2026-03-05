package com.club.sla.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class DashboardController {

  private final DashboardQueryService dashboardQueryService;

  public DashboardController(DashboardQueryService dashboardQueryService) {
    this.dashboardQueryService = dashboardQueryService;
  }

  @GetMapping("/{repositoryId}/dashboard")
  public DashboardSummaryDto dashboard(@PathVariable Long repositoryId) {
    return dashboardQueryService.fetch(repositoryId);
  }
}

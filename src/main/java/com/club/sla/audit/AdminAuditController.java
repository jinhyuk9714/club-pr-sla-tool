package com.club.sla.audit;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {

  private final AdminAuditLogService adminAuditLogService;

  public AdminAuditController(AdminAuditLogService adminAuditLogService) {
    this.adminAuditLogService = adminAuditLogService;
  }

  @GetMapping
  public List<AdminAuditLogDto> list(
      @RequestParam(required = false) String operation,
      @RequestParam(defaultValue = "50") int limit) {
    return adminAuditLogService.list(operation, limit);
  }
}

package com.club.sla.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

  List<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<AdminAuditLog> findByOperationOrderByCreatedAtDesc(String operation, Pageable pageable);
}

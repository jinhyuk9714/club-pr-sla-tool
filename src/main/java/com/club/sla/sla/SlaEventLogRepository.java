package com.club.sla.sla;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaEventLogRepository extends JpaRepository<SlaEventLog, Long> {

  boolean existsByRepoIdAndPrNumberAndStage(Long repoId, Long prNumber, SlaAction stage);
}

package com.club.sla.sla;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaEventLogRepository extends JpaRepository<SlaEventLog, Long> {

  boolean existsByRepoIdAndPrNumberAndStage(Long repoId, Long prNumber, SlaAction stage);

  List<SlaEventLog> findByRepoIdAndPrNumber(Long repoId, Long prNumber);
}

package com.club.sla.notify;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetterEvent, Long> {

  List<DeadLetterEvent> findByReplayStatusOrderByCreatedAtDesc(
      DeadLetterReplayStatus replayStatus, Pageable pageable);
}

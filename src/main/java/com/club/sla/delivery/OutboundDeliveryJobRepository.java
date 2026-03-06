package com.club.sla.delivery;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundDeliveryJobRepository extends JpaRepository<OutboundDeliveryJob, Long> {

  Optional<OutboundDeliveryJob> findByUniqueKeyAndStatusIn(
      String uniqueKey, Collection<OutboundDeliveryJobStatus> statuses);

  List<OutboundDeliveryJob> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      OutboundDeliveryJobStatus status, Instant nextAttemptAt);
}

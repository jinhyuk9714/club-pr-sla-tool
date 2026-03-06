package com.club.sla.ops;

import com.club.sla.delivery.OutboundDeliveryJob;
import com.club.sla.delivery.OutboundDeliveryJobRepository;
import com.club.sla.delivery.OutboundDeliveryJobStatus;
import com.club.sla.delivery.OutboundDeliveryJobType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OpsDeliveryQueryService {

  private final OutboundDeliveryJobRepository outboundDeliveryJobRepository;

  public OpsDeliveryQueryService(OutboundDeliveryJobRepository outboundDeliveryJobRepository) {
    this.outboundDeliveryJobRepository = outboundDeliveryJobRepository;
  }

  public java.util.List<OpsDeliverySummaryView> listRecentDeliveries(
      OutboundDeliveryJobStatus status, OutboundDeliveryJobType jobType, Long installationId) {
    return outboundDeliveryJobRepository
        .findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream()
        .filter(job -> status == null || job.getStatus() == status)
        .filter(job -> jobType == null || job.getJobType() == jobType)
        .filter(job -> installationId == null || installationId.equals(job.getInstallationId()))
        .map(this::toView)
        .toList();
  }

  private OpsDeliverySummaryView toView(OutboundDeliveryJob job) {
    return new OpsDeliverySummaryView(
        job.getId(),
        job.getJobType(),
        job.getStatus(),
        job.getInstallationId(),
        job.getRepositoryId(),
        job.getPrNumber(),
        job.getAttemptCount(),
        job.getNextAttemptAt(),
        job.getLastError(),
        job.getCreatedAt());
  }
}

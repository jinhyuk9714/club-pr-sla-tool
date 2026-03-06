package com.club.sla.notify;

import com.club.sla.delivery.OutboundDeliveryJobService;
import com.club.sla.metrics.SlaMetrics;
import com.club.sla.sla.SlaEventLogRepository;
import org.springframework.stereotype.Service;

@Service
public class SlaNotificationService {

  private final OutboundDeliveryJobService outboundDeliveryJobService;
  private final SlaEventLogRepository slaEventLogRepository;
  private final SlaMetrics slaMetrics;

  public SlaNotificationService(
      OutboundDeliveryJobService outboundDeliveryJobService,
      SlaEventLogRepository slaEventLogRepository,
      SlaMetrics slaMetrics) {
    this.outboundDeliveryJobService = outboundDeliveryJobService;
    this.slaEventLogRepository = slaEventLogRepository;
    this.slaMetrics = slaMetrics;
  }

  public void dispatch(NotificationMessage message) {
    if (slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(
        message.repoId(), message.prNumber(), message.stage())) {
      return;
    }

    outboundDeliveryJobService.enqueueDiscordNotification(message);
  }
}

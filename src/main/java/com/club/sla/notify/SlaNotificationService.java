package com.club.sla.notify;

import com.club.sla.metrics.SlaMetrics;
import com.club.sla.sla.SlaEventLog;
import com.club.sla.sla.SlaEventLogRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class SlaNotificationService {

  private final NotificationPort notificationPort;
  private final SlaEventLogRepository slaEventLogRepository;
  private final SlaMetrics slaMetrics;
  private final Clock clock = Clock.systemUTC();

  public SlaNotificationService(
      NotificationPort notificationPort,
      SlaEventLogRepository slaEventLogRepository,
      SlaMetrics slaMetrics) {
    this.notificationPort = notificationPort;
    this.slaEventLogRepository = slaEventLogRepository;
    this.slaMetrics = slaMetrics;
  }

  public void dispatch(NotificationMessage message) {
    if (slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(
        message.repoId(), message.prNumber(), message.stage())) {
      return;
    }

    notificationPort.send(message);
    slaMetrics.incrementNotification(message.stage());

    try {
      slaEventLogRepository.save(
          new SlaEventLog(
              message.repoId(), message.prNumber(), message.stage(), Instant.now(clock)));
    } catch (DataIntegrityViolationException ignored) {
      // Concurrent duplicate dispatch is treated as already sent.
    }
  }
}

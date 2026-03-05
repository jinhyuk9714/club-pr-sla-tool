package com.club.sla.notify;

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
  private final Clock clock = Clock.systemUTC();

  public SlaNotificationService(
      NotificationPort notificationPort, SlaEventLogRepository slaEventLogRepository) {
    this.notificationPort = notificationPort;
    this.slaEventLogRepository = slaEventLogRepository;
  }

  public void dispatch(NotificationMessage message) {
    if (slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(
        message.repoId(), message.prNumber(), message.stage())) {
      return;
    }

    notificationPort.send(message);

    try {
      slaEventLogRepository.save(
          new SlaEventLog(
              message.repoId(), message.prNumber(), message.stage(), Instant.now(clock)));
    } catch (DataIntegrityViolationException ignored) {
      // Concurrent duplicate dispatch is treated as already sent.
    }
  }
}

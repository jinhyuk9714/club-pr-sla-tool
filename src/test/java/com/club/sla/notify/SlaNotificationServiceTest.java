package com.club.sla.notify;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLog;
import com.club.sla.sla.SlaEventLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SlaNotificationServiceTest {

  @Mock private NotificationPort notificationPort;

  @Mock private SlaEventLogRepository slaEventLogRepository;

  @InjectMocks private SlaNotificationService slaNotificationService;

  @Test
  void sendsNotificationOnlyOnceForSameStage() {
    NotificationMessage message = new NotificationMessage(1L, 10L, SlaAction.REMIND_12H);

    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(1L, 10L, SlaAction.REMIND_12H))
        .thenReturn(false)
        .thenReturn(true);

    slaNotificationService.dispatch(message);
    slaNotificationService.dispatch(message);

    verify(notificationPort, times(1)).send(message);
    verify(slaEventLogRepository, times(1)).save(any(SlaEventLog.class));
  }

  @Test
  void sendsNotificationsForDifferentStages() {
    NotificationMessage remind = new NotificationMessage(1L, 10L, SlaAction.REMIND_12H);
    NotificationMessage escalate = new NotificationMessage(1L, 10L, SlaAction.ESCALATE_24H);

    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(1L, 10L, SlaAction.REMIND_12H))
        .thenReturn(false);
    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(1L, 10L, SlaAction.ESCALATE_24H))
        .thenReturn(false);

    slaNotificationService.dispatch(remind);
    slaNotificationService.dispatch(escalate);

    verify(notificationPort, times(1)).send(remind);
    verify(notificationPort, times(1)).send(escalate);
    verify(slaEventLogRepository, times(2)).save(any(SlaEventLog.class));
  }

  @Test
  void ignoresRaceConditionUniqueViolationWithoutRetrying() {
    NotificationMessage message = new NotificationMessage(1L, 10L, SlaAction.REMIND_12H);

    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(1L, 10L, SlaAction.REMIND_12H))
        .thenReturn(false);
    doThrow(new DataIntegrityViolationException("duplicate"))
        .when(slaEventLogRepository)
        .save(any(SlaEventLog.class));

    slaNotificationService.dispatch(message);

    verify(notificationPort, times(1)).send(message);
    verify(slaEventLogRepository, times(1)).save(any(SlaEventLog.class));
  }

  @Test
  void doesNotSendWhenStageAlreadyLogged() {
    NotificationMessage message = new NotificationMessage(5L, 20L, SlaAction.ESCALATE_24H);

    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(5L, 20L, SlaAction.ESCALATE_24H))
        .thenReturn(true);

    slaNotificationService.dispatch(message);

    verify(notificationPort, never()).send(any(NotificationMessage.class));
    verify(slaEventLogRepository, never()).save(any(SlaEventLog.class));
  }
}

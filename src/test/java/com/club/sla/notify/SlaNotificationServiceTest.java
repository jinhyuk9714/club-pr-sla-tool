package com.club.sla.notify;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.sla.delivery.OutboundDeliveryJobService;
import com.club.sla.metrics.SlaMetrics;
import com.club.sla.sla.SlaAction;
import com.club.sla.sla.SlaEventLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlaNotificationServiceTest {

  @Mock private OutboundDeliveryJobService outboundDeliveryJobService;

  @Mock private SlaEventLogRepository slaEventLogRepository;

  @Mock private SlaMetrics slaMetrics;

  @InjectMocks private SlaNotificationService slaNotificationService;

  @Test
  void sendsNotificationOnlyOnceForSameStage() {
    NotificationMessage message = new NotificationMessage(1L, 10L, SlaAction.REMIND_12H);

    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(1L, 10L, SlaAction.REMIND_12H))
        .thenReturn(false)
        .thenReturn(true);

    slaNotificationService.dispatch(message);
    slaNotificationService.dispatch(message);

    verify(outboundDeliveryJobService, times(1)).enqueueDiscordNotification(message);
    verify(slaMetrics, never()).incrementNotification(SlaAction.REMIND_12H);
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

    verify(outboundDeliveryJobService, times(1)).enqueueDiscordNotification(remind);
    verify(outboundDeliveryJobService, times(1)).enqueueDiscordNotification(escalate);
    verify(slaMetrics, never()).incrementNotification(any(SlaAction.class));
  }

  @Test
  void doesNotSendWhenStageAlreadyLogged() {
    NotificationMessage message = new NotificationMessage(5L, 20L, SlaAction.ESCALATE_24H);

    when(slaEventLogRepository.existsByRepoIdAndPrNumberAndStage(5L, 20L, SlaAction.ESCALATE_24H))
        .thenReturn(true);

    slaNotificationService.dispatch(message);

    verify(outboundDeliveryJobService, never())
        .enqueueDiscordNotification(any(NotificationMessage.class));
    verify(slaMetrics, never()).incrementNotification(any(SlaAction.class));
  }
}

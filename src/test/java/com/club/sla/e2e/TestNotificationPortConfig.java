package com.club.sla.e2e;

import com.club.sla.notify.NotificationMessage;
import com.club.sla.notify.NotificationPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestNotificationPortConfig {

  @Bean
  RecordingNotificationPort recordingNotificationPort() {
    return new RecordingNotificationPort();
  }

  @Bean
  @Primary
  NotificationPort notificationPort(RecordingNotificationPort recordingNotificationPort) {
    return recordingNotificationPort;
  }

  static class RecordingNotificationPort implements NotificationPort {

    private final List<NotificationMessage> messages = new ArrayList<>();

    @Override
    public synchronized void send(NotificationMessage message) {
      messages.add(message);
    }

    synchronized List<NotificationMessage> allMessages() {
      return List.copyOf(messages);
    }

    synchronized void clear() {
      messages.clear();
    }
  }
}

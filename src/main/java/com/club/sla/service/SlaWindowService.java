package com.club.sla.service;

import java.time.Duration;
import java.time.Instant;

public final class SlaWindowService {

  private SlaWindowService() {}

  public static Duration elapsed(Instant readyAt, Instant now) {
    return Duration.between(readyAt, now);
  }
}

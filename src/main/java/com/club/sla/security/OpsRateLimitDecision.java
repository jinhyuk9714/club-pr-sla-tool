package com.club.sla.security;

public record OpsRateLimitDecision(boolean allowed, long retryAfterSeconds) {

  public static OpsRateLimitDecision allow() {
    return new OpsRateLimitDecision(true, 0L);
  }

  public static OpsRateLimitDecision deny(long retryAfterSeconds) {
    return new OpsRateLimitDecision(false, retryAfterSeconds);
  }
}

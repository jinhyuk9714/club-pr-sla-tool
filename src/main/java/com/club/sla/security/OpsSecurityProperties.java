package com.club.sla.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ops.security")
public class OpsSecurityProperties {

  private String adminApiKey = "";
  private final RateLimit rateLimit = new RateLimit();

  public String getAdminApiKey() {
    return adminApiKey;
  }

  public void setAdminApiKey(String adminApiKey) {
    this.adminApiKey = adminApiKey;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public static class RateLimit {
    private int maxRequests = 60;
    private int windowSeconds = 60;

    public int getMaxRequests() {
      return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
      this.maxRequests = maxRequests;
    }

    public int getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
      this.windowSeconds = windowSeconds;
    }
  }
}

package com.club.sla.ops;

public record OpsUiLoginResult(
    boolean authenticated, boolean rateLimited, long retryAfterSeconds, String errorMessage) {

  public static OpsUiLoginResult success() {
    return new OpsUiLoginResult(true, false, 0, null);
  }

  public static OpsUiLoginResult invalidCredentials() {
    return new OpsUiLoginResult(false, false, 0, "Invalid admin API key.");
  }

  public static OpsUiLoginResult rateLimited(long retryAfterSeconds) {
    return new OpsUiLoginResult(
        false, true, retryAfterSeconds, "Too many login attempts. Try again later.");
  }
}

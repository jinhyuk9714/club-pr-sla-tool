package com.club.sla.sla;

public record SlaEvaluation(SlaAction action) {

  public static SlaEvaluation none() {
    return new SlaEvaluation(null);
  }

  public static SlaEvaluation of(SlaAction action) {
    return new SlaEvaluation(action);
  }

  public boolean hasAction() {
    return action != null;
  }
}

package com.club.sla.notify;

import com.club.sla.sla.SlaAction;

public record NotificationMessage(Long repoId, Long prNumber, SlaAction stage) {}

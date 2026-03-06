package com.club.sla.delivery;

import com.club.sla.sla.SlaAction;

public record DiscordNotificationJobPayload(Long repositoryId, Long prNumber, SlaAction stage) {}

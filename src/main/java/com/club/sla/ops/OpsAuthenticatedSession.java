package com.club.sla.ops;

import java.time.Instant;

public record OpsAuthenticatedSession(String subject, Instant authenticatedAt) {}
